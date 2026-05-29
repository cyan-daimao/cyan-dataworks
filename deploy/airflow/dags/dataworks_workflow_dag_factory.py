from __future__ import annotations

import logging
import os
from datetime import datetime

import pendulum
import requests
from airflow import DAG
from airflow.exceptions import AirflowException
from airflow.providers.http.operators.http import HttpOperator
from airflow.sensors.external_task import ExternalTaskSensor
from airflow.sensors.python import PythonSensor
from airflow.utils.state import DagRunState
from airflow.utils.task_group import TaskGroup


DATAWORKS_BASE_URL = os.getenv("DATAWORKS_BASE_URL", "http://cyan-dataworks-svc.pre.svc.cluster.local:8080")
DAG_DEFINITION_ENDPOINT = "/rpc/dataworks/airflow/workflow-dag-definitions"
LOG = logging.getLogger(__name__)


def _load_dag_definitions() -> list[dict]:
    url = f"{DATAWORKS_BASE_URL}{DAG_DEFINITION_ENDPOINT}"
    LOG.info("Loading DataWorks workflow DAG definitions from %s", url)
    try:
        response = requests.get(url, timeout=10)
        response.raise_for_status()
        payload = response.json()
    except Exception:
        LOG.exception("Failed to load DataWorks workflow DAG definitions from %s", url)
        return []
    definitions = payload.get("data") or []
    LOG.info("Loaded %s DataWorks workflow DAG definitions", len(definitions))
    return definitions


def _make_task_payload(dag_id: str, task_id: str) -> str:
    return """
{
  "schedulerType": "AIRFLOW",
  "dagId": "%s",
  "dagRunId": "{{ dag_run.run_id }}",
  "taskId": "%s",
  "logicalDate": "{{ logical_date.strftime('%%Y-%%m-%%dT%%H:%%M:%%S') }}",
  "tryNumber": {{ ti.try_number }}
}
""" % (dag_id, task_id)


def _check_task_response(response) -> bool:
    try:
        payload = response.json()
    except Exception:
        LOG.exception("DataWorks scheduler response is not valid JSON: status_code=%s", response.status_code)
        return False
    data = payload.get("data") or {}
    success = payload.get("code") == 200 and bool(data.get("id"))
    if not success:
        LOG.error("DataWorks scheduler submit failed: %s", payload)
    return success


def _extract_instance_id(response) -> str:
    payload = response.json()
    data = payload.get("data") or {}
    instance_id = data.get("id")
    if not instance_id:
        raise AirflowException(f"DataWorks scheduler response does not contain instance id: {payload}")
    return instance_id


def _external_wait_task_id(dependency: dict, index: int) -> str:
    upstream_workflow_id = str(dependency.get("upstreamWorkflowId") or "").strip()
    if upstream_workflow_id:
        return f"wait_for_workflow_{upstream_workflow_id}"
    upstream_dag_id = str(dependency.get("upstreamDagId") or "").strip()
    if upstream_dag_id:
        return f"wait_for_{upstream_dag_id}".replace("-", "_").replace(".", "_")
    return f"wait_for_external_workflow_{index + 1}"


def _wait_instance_success(submit_task_id: str, **context) -> bool:
    instance_id = context["ti"].xcom_pull(task_ids=submit_task_id)
    if not instance_id:
        LOG.warning("DataWorks instance id is not available yet: submit_task_id=%s", submit_task_id)
        return False
    url = f"{DATAWORKS_BASE_URL}/rpc/dataworks/job-instances/{instance_id}/scheduler-status"
    try:
        response = requests.get(url, timeout=10)
        response.raise_for_status()
        payload = response.json()
    except Exception:
        LOG.exception("Failed to query DataWorks instance status: instance_id=%s", instance_id)
        return False
    data = payload.get("data") or {}
    status = data.get("status")
    if status == "SUCCESS":
        return True
    if status == "FAILED":
        raise AirflowException(f"DataWorks instance failed: instance_id={instance_id}, payload={payload}")
    LOG.info("DataWorks instance is still running: instance_id=%s, status=%s", instance_id, status)
    return False


def _normalize_cron_expression(cron_expression: str | None) -> str | None:
    if not cron_expression:
        return None
    raw_parts = cron_expression.strip().split()
    if not raw_parts:
        return None
    parts = [part.replace("?", "*").replace("？", "*") for part in raw_parts]
    if len(parts) == 5:
        if raw_parts[-1].endswith("?") or raw_parts[-1].endswith("？"):
            normalized = [parts[1], parts[2], parts[3], "*", "*"]
        else:
            normalized = parts
    elif len(parts) in (6, 7):
        normalized = parts[1:6]
    else:
        LOG.warning("Unsupported DataWorks cron expression: %s", cron_expression)
        return None
    LOG.info("Normalized DataWorks cron expression: raw=%s, normalized=%s", cron_expression, " ".join(normalized))
    return " ".join(normalized)


for dag_def in _load_dag_definitions():
    try:
        dag_id = dag_def["dagId"]
        schedule = _normalize_cron_expression(dag_def.get("cronExpression"))
        LOG.info(
            "Registering DataWorks Workflow DAG: dag_id=%s, workflow_id=%s, raw_cron=%s, schedule=%s, task_count=%s",
            dag_id,
            dag_def.get("workflowId"),
            dag_def.get("cronExpression"),
            schedule,
            len(dag_def.get("tasks") or []),
        )
        with DAG(
            dag_id=dag_id,
            start_date=datetime(2026, 1, 1, tzinfo=pendulum.timezone("Asia/Shanghai")),
            schedule=schedule,
            catchup=False,
            is_paused_upon_creation=not bool(dag_def.get("scheduleEnabled")),
            tags=["dataworks", "workflow"],
        ) as dag:
            task_handles = {}
            external_wait_tasks = []
            for index, dependency in enumerate(dag_def.get("externalDependencies") or []):
                upstream_dag_id = dependency.get("upstreamDagId")
                if not upstream_dag_id:
                    continue
                external_wait_tasks.append(
                    ExternalTaskSensor(
                        task_id=_external_wait_task_id(dependency, index),
                        external_dag_id=upstream_dag_id,
                        external_task_id=None,
                        allowed_states=[DagRunState.SUCCESS],
                        failed_states=[DagRunState.FAILED],
                        mode="reschedule",
                        poke_interval=30,
                        timeout=60 * 60 * 24,
                    )
                )
            for task_def in dag_def.get("tasks") or []:
                task_id = task_def["taskId"]
                workflow_id = dag_def["workflowId"]
                node_id = task_def["nodeId"]
                with TaskGroup(group_id=task_id) as task_group:
                    submit = HttpOperator(
                        task_id="submit",
                        http_conn_id="dataworks_http",
                        endpoint=f"/rpc/dataworks/workflows/{workflow_id}/nodes/{node_id}/run-by-scheduler",
                        method="POST",
                        data=_make_task_payload(dag_id, task_id),
                        headers={"Content-Type": "application/json", "Accept": "application/json"},
                        response_check=_check_task_response,
                        response_filter=_extract_instance_id,
                        log_response=True,
                    )
                    wait = PythonSensor(
                        task_id="wait",
                        python_callable=_wait_instance_success,
                        op_kwargs={"submit_task_id": f"{task_id}.submit"},
                        mode="reschedule",
                        poke_interval=10,
                        timeout=60 * 60 * 24,
                    )
                    submit >> wait
                task_handles[task_id] = {"group": task_group, "submit": submit, "wait": wait}
            for task_def in dag_def.get("tasks") or []:
                task = task_handles[task_def["taskId"]]
                for upstream_task_id in task_def.get("upstreamTaskIds") or []:
                    if upstream_task_id not in task_handles:
                        raise ValueError(f"Unknown upstream task {upstream_task_id} for task {task_def['taskId']}")
                    task_handles[upstream_task_id]["wait"] >> task["submit"]
            root_task_ids = [
                task_def["taskId"]
                for task_def in dag_def.get("tasks") or []
                if not task_def.get("upstreamTaskIds")
            ]
            for external_wait in external_wait_tasks:
                for root_task_id in root_task_ids:
                    external_wait >> task_handles[root_task_id]["submit"]
        globals()[dag_id] = dag
    except Exception:
        LOG.exception("Skip invalid DataWorks workflow DAG definition: %s", dag_def)
