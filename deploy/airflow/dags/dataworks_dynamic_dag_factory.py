from __future__ import annotations

import os
import logging
from datetime import datetime

import pendulum
import requests
from airflow import DAG
from airflow.providers.http.operators.http import HttpOperator


DATAWORKS_BASE_URL = os.getenv("DATAWORKS_BASE_URL", "http://cyan-dataworks.pre.svc.cluster.local:8080")
DAG_DEFINITION_ENDPOINT = os.getenv("DATAWORKS_DAG_DEFINITION_ENDPOINT", "/rpc/dataworks/airflow/dag-definitions")
LOG = logging.getLogger(__name__)


def _load_dag_definitions() -> list[dict]:
    url = f"{DATAWORKS_BASE_URL}{DAG_DEFINITION_ENDPOINT}"
    LOG.info("Loading DataWorks DAG definitions from %s", url)
    try:
        response = requests.get(
            url,
            timeout=10,
        )
        response.raise_for_status()
        payload = response.json()
    except Exception:
        LOG.exception("Failed to load DataWorks DAG definitions from %s", url)
        return []
    definitions = payload.get("data") or []
    LOG.info("Loaded %s DataWorks DAG definitions", len(definitions))
    return definitions


def _make_task_payload(job_id: str, dag_id: str, task_id: str) -> str:
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
    success = payload.get("code") == 200 and data.get("status") == "SUCCESS"
    if not success:
        LOG.error("DataWorks scheduler task failed: %s", payload)
    return success


def _normalize_cron_expression(cron_expression: str | None) -> str | None:
    if not cron_expression:
        return None
    raw_parts = cron_expression.strip().split()
    if not raw_parts:
        return None
    parts = [part.replace("?", "*") for part in raw_parts]
    if len(parts) == 5:
        if raw_parts[-1].endswith("?"):
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
            "Registering DataWorks DAG: dag_id=%s, job_id=%s, raw_cron=%s, schedule=%s, task_count=%s",
            dag_id,
            dag_def.get("jobId"),
            dag_def.get("cronExpression"),
            schedule,
            len(dag_def.get("tasks") or []),
        )
        with DAG(
            dag_id=dag_id,
            start_date=datetime(2026, 1, 1, tzinfo=pendulum.timezone("Asia/Shanghai")),
            schedule=schedule,
            catchup=False,
            tags=["dataworks"],
        ) as dag:
            tasks = {}
            for task_def in dag_def.get("tasks") or []:
                task_id = task_def["taskId"]
                job_id = task_def["jobId"]
                tasks[task_id] = HttpOperator(
                    task_id=task_id,
                    http_conn_id="dataworks_http",
                    endpoint=f"/rpc/dataworks/job-instances/{job_id}/run-by-scheduler",
                    method="POST",
                    data=_make_task_payload(job_id, dag_id, task_id),
                    headers={"Content-Type": "application/json", "Accept": "application/json"},
                    response_check=_check_task_response,
                    log_response=True,
                )
            for task_def in dag_def.get("tasks") or []:
                task = tasks[task_def["taskId"]]
                for upstream_task_id in task_def.get("upstreamTaskIds") or []:
                    if upstream_task_id not in tasks:
                        raise ValueError(f"Unknown upstream task {upstream_task_id} for task {task_def['taskId']}")
                    tasks[upstream_task_id] >> task
        globals()[dag_id] = dag
    except Exception:
        LOG.exception("Skip invalid DataWorks DAG definition: %s", dag_def)
