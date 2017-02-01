task make_file {
  command {
    echo There once was a fisher named Fisher > out
    echo who fished for some fish in a fissure >> out
    echo Till a fish with a grin >> out
    echo pulled the fisherman in >> out
    echo Now theyre fishing the fissure for Fisher >> out
  }
  runtime {
    docker: "ubuntu:latest"
  }
  output {
    File out = "out"
  }
}

task maybe_cat {
  File? opt_file

  command {
    ${ "cat " + opt_file }
  }
  runtime {
    docker: "ubuntu:latest"
  }
  output {
    String result = read_string(stdout())
  }
}

workflow optional_files {
  if (true) { call make_file as make_true }
  if (false) { call make_file as make_false }

  call maybe_cat as maybe_true { input: opt_file = make_true.out }
  call maybe_cat as maybe_false { input: opt_file = make_false.out }

  output {
    Array[String] result = [ maybe_true.result, maybe_false.result ]
  }
}
