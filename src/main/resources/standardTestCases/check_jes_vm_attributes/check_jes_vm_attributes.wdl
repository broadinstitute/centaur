task check_preemption_specified {
  # This checks preemption in the GCE VM metadata, not what was requested of JES.  This should be more rigorous than
  # checking JES metadata since it confirms the preemption flag was correctly passed to the VM.
  Int specified_preemptible

  command {
    curl "http://metadata.google.internal/computeMetadata/v1/instance/scheduling/preemptible" -H "Metadata-Flavor: Google"
  }

  output {
    String out = read_string(stdout())
  }

  runtime {
    preemptible: "${specified_preemptible}"
    # includes curl
    docker: "python:latest"
  }
}

task check_preemption_unspecified {
  # This checks preemption in the GCE VM metadata, not what was requested of JES.  This should be more rigorous than
  # checking JES metadata since it confirms the preemption flag was correctly passed to the VM.
  command {
    curl "http://metadata.google.internal/computeMetadata/v1/instance/scheduling/preemptible" -H "Metadata-Flavor: Google"
  }
  output {
    String out = read_string(stdout())
  }

  runtime {
    # includes curl
    docker: "python:latest"
  }
}

task check_cpus {
  # This checks actual CPUs on the machine, not GCE VM or JES metadata.  The actual number of CPUs may
  # differ from what was requested by Cromwell due to the granular GCE VM sizing.
  #
  # https://cloud.google.com/compute/docs/machine-types

  Int specified_cpus

  command {
    cat /proc/cpuinfo | grep -P '^processor\s+:\s+\d+' | wc -l
  }
  output {
    Int out = read_int(stdout())
  }
  runtime {
    docker: "ubuntu:latest"
    cpu: "${specified_cpus}"
  }
}

task check_memory_gb {
  # This checks JES metadata for the memory requested by Cromwell, not the actual memory on the machine.  Due to
  # GCE VM sizing the actual amount of memory on the machine will likely be very different from what was requested.
  #
  # https://cloud.google.com/compute/docs/machine-types

  Int specified_memory_gb

  command <<<

    cat > script.py <<EOF
    import httplib, urllib
    params = urllib.urlencode({})
    headers = {"Metadata-Flavor": "Google"}
    conn = httplib.HTTPConnection("metadata.google.internal")
    conn.request("GET", "/computeMetadata/v1/instance/description", params, headers)
    res = conn.getresponse()
    data = res.read()
    print data
    EOF

    python script.py | perl -ne '/Operation:\s+(.*)/ && print $1' | xargs gcloud alpha genomics operations describe | grep minimumRamGb | sort -u | grep -Po '\d+$'

  >>>

  output {
    Int out = read_int(stdout())
  }
  runtime {
    docker: "google/cloud-sdk"
    memory: "${specified_memory_gb} GB"
  }
}

task check_zone_unspecified {
  # This checks GCE VM data for zone, not what was requested of JES.  A JES metadata check might also be worthwhile since
  # Cromwell can request more than one zone as a possibility for execution, but obviously a given VM instance can only
  # be running in one zone.
  command {
    curl -s "http://metadata.google.internal/computeMetadata/v1/instance/zone" -H "Metadata-Flavor: Google" | grep -Po '[^/]+$'
  }
  output {
    String out = read_string(stdout())
  }
  runtime {
    docker: "python:latest"
  }
}

task check_zone_specified {
  # This checks GCE VM data for zone, not what was requested of JES.  A JES metadata check might also be worthwhile since
  # Cromwell can request more than one zone as a possibility for execution, but obviously a given VM instance can only
  # be running in one zone.
  String specified_zone

  command {
    curl -s "http://metadata.google.internal/computeMetadata/v1/instance/zone" -H "Metadata-Flavor: Google" | grep -Po '[^/]+$'
  }
  output {
    String out = read_string(stdout())
  }
  runtime {
    docker: "python:latest"
    zones: "${specified_zone}"
  }
}

task check_no_address_specified {
  # This checks JES metadata for the 'noAddress' GCE Private IP attribute.  This does not check the VM metadata directly
  # since the attribute does not show up at all when it is not set to true, which makes it harder to positively test.

  # On the 'broad-dsde-cromwell-dev' project this will currently hang if `noAddress` is set to true since that project
  # has not yet been whitelisted for GCE Private IPs.
  Boolean no_address_specified

  command <<<

    cat > script.py <<EOF
    import httplib, urllib
    params = urllib.urlencode({})
    headers = {"Metadata-Flavor": "Google"}
    conn = httplib.HTTPConnection("metadata.google.internal")
    conn.request("GET", "/computeMetadata/v1/instance/description", params, headers)
    res = conn.getresponse()
    data = res.read()
    print data
    EOF

    python script.py | perl -ne '/Operation:\s+(.*)/ && print $1' | xargs gcloud alpha genomics operations describe | perl -ne '/^\s+noAddress\s*:\s*(.*)\s*/ && print $1'

  >>>

  output {
    String out = read_string(stdout())
  }
  runtime {
    docker: "google/cloud-sdk"
    noAddress: "${no_address_specified}"
  }
}

task check_no_address_unspecified {
  # This checks JES metadata for the 'noAddress' GCE Private IP attribute.  This does not check the VM metadata directly
  # since the attribute does not show up at all when it is not set to true, which makes it harder to positively test.
  command <<<

    cat > script.py <<EOF
    import httplib, urllib
    params = urllib.urlencode({})
    headers = {"Metadata-Flavor": "Google"}
    conn = httplib.HTTPConnection("metadata.google.internal")
    conn.request("GET", "/computeMetadata/v1/instance/description", params, headers)
    res = conn.getresponse()
    data = res.read()
    print data
    EOF

    python script.py | perl -ne '/Operation:\s+(.*)/ && print $1' | xargs gcloud alpha genomics operations describe | perl -ne '/^\s+noAddress\s*:\s*(.*)\s*/ && print $1'

  >>>

  output {
    String out = read_string(stdout())
  }
  runtime {
    docker: "google/cloud-sdk"
  }
}


workflow w {

  call check_preemption_specified as check_preemption_specified_one  { input: specified_preemptible = 1 }
  call check_preemption_specified as check_preemption_specified_zero { input: specified_preemptible = 0 }
  call check_preemption_unspecified

  call check_cpus as check_cpus_specified_one { input: specified_cpus = 1 }
  call check_cpus as check_cpus_specified_two { input: specified_cpus = 2 }

  call check_memory_gb as check_memory_specified_two   { input: specified_memory_gb = 2 }
  call check_memory_gb as check_memory_specified_four  { input: specified_memory_gb = 4 }

  call check_zone_unspecified
  call check_zone_specified { input: specified_zone = "us-east1-c" }

  #call check_no_address_specified as check_no_address_specified_true { input: no_address_specified = true }
  call check_no_address_specified as check_no_address_specified_false { input: no_address_specified = false }
  call check_no_address_unspecified

  output {

    String preemptible_specified_one  = check_preemption_specified_one.out
    String preemptible_specified_zero = check_preemption_specified_zero.out
    String preemptible_unspecified    = check_preemption_unspecified.out

    Int cpu_specified_one = check_cpus_specified_one.out
    Int cpu_specified_two = check_cpus_specified_two.out

    Int memory_specified_two_gb  = check_memory_specified_two.out
    Int memory_specified_four_gb = check_memory_specified_four.out

    String zone_unspecified = check_zone_unspecified.out
    String zone_specified = check_zone_specified.out

    #String no_address_specified_true = check_no_address_specified_true.out
    String no_address_specified_false = check_no_address_specified_false.out
    String no_address_unspecified = check_no_address_unspecified.out
  }
}
