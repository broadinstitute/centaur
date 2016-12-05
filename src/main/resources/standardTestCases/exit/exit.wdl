task exitTask {
  command {
    exit 5
    sleep 2
  }
  runtime {
    docker: "ubuntu:latest"
    continueOnReturnCode: 5
  }
}

workflow exitWorkflow {
  call exitTask
}
