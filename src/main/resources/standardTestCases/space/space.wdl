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
    File file_input2 = "gs://cloud-cromwell-dev/travis-centaur/file%20name%20with%20.txt"
    String file_input_content = read_string(file_input)
    String file_input_content2 = read_string(file_input2)
    
    call hello { input: f = file_input }
    call hello as hello2 { input: f = file_input2 }
    call goodbye { input: f = hello2.single, files = hello2.globbed }
    call goodbye as goodbye2 { input: f = hello2.single, files = hello2.globbed }
    
    output {
        String o1 = file_input_content
        String o2 = file_input_content2
        String o3 = goodbye.out
        String o4 = goodbye2.out
    }
}
