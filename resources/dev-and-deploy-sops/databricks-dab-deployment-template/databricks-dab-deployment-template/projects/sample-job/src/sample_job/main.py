"""Job entry point.

The entry point is the ONLY layer that knows about Spark and the
environment. It receives the catalog as a parameter (passed by the
job definition in resources/sample_job.yml) and delegates all real
logic to transforms.py.
"""
import argparse


def run() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--catalog", required=True)
    args = parser.parse_args()

    from pyspark.sql import SparkSession
    from pyspark.sql import functions as F
    from shared_core.transforms import SEVERITY_MAP

    spark = SparkSession.builder.getOrCreate()
    mapping = F.create_map(
        *[x for k, v in SEVERITY_MAP.items() for x in (F.lit(k), F.lit(v))]
    )
    (
        spark.table(f"{args.catalog}.bronze.findings")
        .withColumn("severity_rank", mapping[F.upper(F.col("severity"))])
        .write.mode("overwrite")
        .saveAsTable(f"{args.catalog}.silver.findings")
    )


if __name__ == "__main__":
    run()
