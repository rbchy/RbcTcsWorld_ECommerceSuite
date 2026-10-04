# Architecture

The project uses a modular Maven root with independent `backend` and `automation` modules plus a React frontend. Backend domains are organized by business capability. Security uses stateless JWT authentication. PostgreSQL is the system of record and Flyway owns schema migrations. Automation is separated from production code so the same APIs can be tested by CI and locally.
