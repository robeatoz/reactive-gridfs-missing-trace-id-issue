# Reactive GridFS missing trace-id issue

This is a minimal example for the issue XXX

Steps to reproduce:

1. Start MongoDB with Docker using the command: ``docker run --rm -p "27017:27017" mongo:7.0.6``.
2. Launch the application ``ReactiveGridFsIssueApplication``.
3. Access the endpoint at http://localhost:8080/workingTraceIdButAllElementsInMemory.
4. Access the endpoint at http://localhost:8080/notWorkingTraceIdButOnlyFewElementsInMemory. 
5. Watch the logs and trace-ids