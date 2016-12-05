task hello {
  String? person

  command {
    echo "hello ${default = "default value" person}"
    sleep 2
  }

  runtime { docker: "ubuntu:latest" }

  output {
    String greeting = read_string(stdout())
  }
}

task hello_no_default {
  String? person

  command {
    echo "hello ${person}"
    sleep 2
  }

  runtime { docker: "ubuntu:latest" }

  output {
    String greeting = read_string(stdout())
  }
}

workflow default {
  call hello
  call hello as hello_no_value
  call hello_no_default
}
