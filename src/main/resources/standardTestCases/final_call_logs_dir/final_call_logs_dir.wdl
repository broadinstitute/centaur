task hello {
  command {
    echo "Hello " > test.out
  }
  output {
    File out = "test.out"
  }
  runtime {
    docker: "ubuntu:latest"
  }
}

workflow final_call_logs_dir {
  call hello
  output {
     hello.out
  }
}
