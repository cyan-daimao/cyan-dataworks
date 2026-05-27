from __future__ import annotations

import os
import logging
from datetime import datetime

import pendulum
import requests
from airflow import DAG
from airflow.providers.http.operators.http import HttpOperator


DATAWORKS_BASE_URL = os.getenv("DATAWORKS_BASE_URL", "http://cyan-dataworks.pre.svc.cluster.local:8080")
DATAWORKS_TOKEN = os.getenv("DATAWORKS_TOKEN", "")
DAG_DEFINITION_ENDPOINT = os.getenv("DATAWORKS_DAG_DEFINITION_ENDPOINT", "/rpc/dataworks/airflow/dag-definitions")
LOG = logging.getLogger(__name__)


def _headers() -> dict[str, str]:
    headers = {"Content-Type": "application/json"}
    if DATAWORKS_TOKEN:
        headers["Authorization"] = f"Bearer {DATAWORKS_TOKEN}"
    return headers


def _load_dag_definitions() -> list[dict]:
    url = f"{DATAWORKS_BASE_URL}{DAG_DEFINITION_ENDPOINT}"
    LOG.info("Loading DataWorks DAG definitions from %s", url)
    try:
        response = requests.get(
            url,
            headers=_headers(),
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


for dag_def in _load_dag_definitions():
    dag_id = dag_def["dagId"]
    LOG.info(
        "Registering DataWorks DAG: dag_id=%s, job_id=%s, cron=%s, task_count=%s",
        dag_id,
        dag_def.get("jobId"),
        dag_def.get("cronExpression"),
        len(dag_def.get("tasks") or []),
    )
    with DAG(
        dag_id=dag_id,
        start_date=datetime(2026, 1, 1, tzinfo=pendulum.timezone("Asia/Shanghai")),
        schedule=dag_def.get("cronExpression"),
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
                endpoint=f"/api/v1/data-work/jobs/{job_id}/run-by-scheduler",
                method="POST",
                data=_make_task_payload(job_id, dag_id, task_id),
                headers=_headers(),
                response_check=lambda response: response.json().get("data", {}).get("status") == "SUCCESS",
                log_response=True,
            )
        for task_def in dag_def.get("tasks") or []:
            task = tasks[task_def["taskId"]]
            for upstream_task_id in task_def.get("upstreamTaskIds") or []:
                tasks[upstream_task_id] >> task
    globals()[dag_id] = dag
