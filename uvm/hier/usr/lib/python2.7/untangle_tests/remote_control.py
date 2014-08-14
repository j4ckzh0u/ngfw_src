import os
import sys
import subprocess
import time
import datetime

# exteral global variables
clientIP = None
hostUsername = "testshell"
hostKeyFile = "/usr/lib/python2.7/untangle_tests/testShell.key"
logfile = None
verbosity = 0
sshOptions = "-o StrictHostKeyChecking=no -o ConnectTimeout=300 -o ConnectionAttempts=15"
quickTestsOnly = False
interface = 0
interfaceExternal = 0

__orig_stdout = None
__orig_stderr = None

# set the key file permissions correctly just in case
os.system("chmod 600 %s" % hostKeyFile)

def __redirectOutput( logfile ):
    global __orig_stderr, __orig_stdout
    __orig_stdout = sys.stdout
    __orig_stderr = sys.stderr
    sys.stdout = logfile
    sys.stderr = logfile

def __restoreOutput():
    global __orig_stderr, __orig_stdout
    sys.stdout = __orig_stdout
    sys.stderr = __orig_stderr

# runs a given command on the specified host (or the default client IP if host = None)
# returns the exit code of the command
# if stdout=True returns the output of the command
# if nowait=True returns the initial output if stdout=True, 0 otherwise
def runCommand( command, host=None, stdout=False, nowait=False):
    global clientIP, hostUsername, hostKeyFile, sshOptions, logfile, verbosity
    if host == None:
        host = clientIP

    if logfile != None:
        __redirectOutput( logfile )

    result = 1
    try:
        sshCommand = "ssh %s -i %s %s@%s \"%s\"" % ( sshOptions, hostKeyFile, hostUsername, host, command )
        # if verbosity > 1:
        #    print "\nSSH cmd : %s" % sshCommand
        if verbosity > 0:
            print "\nClient  : %s" % host
            print "Command : %s" % command
        if (nowait):
            sshCommand += " & " # don't wait for process to complete
        proc = subprocess.Popen(sshCommand, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)

        # If nowait, sleep for a second to give time for the ssh to connect and run the command before returning
        if nowait:
            time.sleep(1)
            if stdout:
                return proc.communicate()[0].strip()
            else:
                return 0

        result = proc.wait()
        output = proc.communicate()[0].strip()
        print "Result  : %i"  % result
        print "Output  : %s"  % output
        sys.stdout.flush()
        if stdout:
            return output
        else:
            return result
    finally:
        if logfile != None:
            __restoreOutput()

def isOnline( host=None ):
    return runCommand("wget -q -O /dev/null -4 -t 2 --timeout=5 http://test.untangle.com/", host=host)

