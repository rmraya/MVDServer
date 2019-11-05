cd "$(dirname "$0")/"

bin/java --module-path lib -m mvdserver/com.maxprograms.mvdserver.MVDServer $@ &
