task hello {
    File f
    String s = read_string(f)
    command {
        echo "Hello ${s}" > 'out with space.txt'
    }
    runtime {
        docker: "ubuntu:latest"
    }
    output {
        File single = "out with space.txt"
        Array[File] globbed = glob("*.txt")
    }
}

task goodbye {
    File f
    Array[File] files
    command {
        echo "Goodbye ${f}"
        echo "Goodbye ${sep = " " files}"
    }
    runtime {
            docker: "ubuntu:latest"
    }
    output {
        String out = read_string(stdout())
    }
}

workflow space {
    File file_input = "gs://cloud-cromwell-dev/travis-centaur/file name with spaces.txt"
    
    call hello { input: f = file_input }
    call goodbye { input: f = hello.single, files = hello.globbed }
}