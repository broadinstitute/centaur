task makeGcsFile {
  command {
    echo "I am writing stuff" > out
  }

  runtime {
    docker: "ubuntu:latest"
  }

  output {
    File outputFile = "out"
  }
}

task readGcsFileFromLocal {
  File gcsFile

  command {
    cat ${gcsFile}
  }

  runtime {
    backend: "Local"
  }

  output {
    String result = read_string(stdout())
  }
}

workflow local_gcs {
  call makeGcsFile
  call readGcsFileFromLocal { input: gcsFile = makeGcsFile.outputFile }
}