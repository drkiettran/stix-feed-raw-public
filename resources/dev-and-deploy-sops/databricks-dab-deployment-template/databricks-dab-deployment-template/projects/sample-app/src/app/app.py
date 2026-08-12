"""Minimal Databricks App (Streamlit).

Demonstrates the two monorepo conventions:
  1. Environment arrives via env vars (set in resources/sample_app.yml) —
     the code is identical in dev, staging, and prod.
  2. Business logic is imported from the SHARED library (shared_core),
     installed from the wheel listed in requirements.txt — never
     re-implemented inside the app.
"""
import os
import streamlit as st
from shared_core.transforms import normalize_severity

CATALOG = os.environ["CATALOG"]

st.title("DAB Template Sample App")
st.write(f"Connected catalog: `{CATALOG}`")

label = st.text_input("Try the shared library: enter a severity label", "HIGH")
st.write(f"`shared_core.normalize_severity` → **{normalize_severity(label)}**")

st.caption(
    "This app runs as its own service principal. If a table query fails "
    "in a new environment, check the APP SP's grants first."
)
