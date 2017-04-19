#!/usr/bin/python
#

#
# This script is run after the Maven build has completed. As a result, all
# build artifacts are already in place (${workspace}/lib/*). The script
# then selects based on input parameters the artifacts that are to be included
# in the resulting RPM and places them into a directory under /var/tmp.
#

from stat import S_ISREG, ST_CTIME, ST_MODE

import argparse
import distutils.dir_util as dir_util
import distutils.file_util as file_util
import glob
import logging
import os
import shutil
import subprocess
import sys
import tarfile
import xml.etree.ElementTree as ETree

# -----------------------------------------------------------------------------------
# Main variable definitions
# -----------------------------------------------------------------------------------

# Name of the project
applicationName = "opencast"

# Path to the database schema
dbSchemaFile = "docs/scripts/ddl/mysql5.sql"

# Whether to keep debuginfo rpms around
rpmDebugInfo = False

# Whether to remove build artifacts upon cleanup / error
noCleanup = False

# The number of artifacts to keep in the rpm repository
rpmHistorySize = 3

# The log level
logLevel = logging.INFO

# -----------------------------------------------------------------------------------
#
# Code section. Don't change anything below here unless you know what you are doing.
#
# -----------------------------------------------------------------------------------

# -----------------------------------------------------------------------------------
# Removes everything that has been created by this build.
#
# @param includeUser
#                    whether to also remove the user
# -----------------------------------------------------------------------------------

def cleanBuildEnvironment():

    # If the global flag has been set to false, then don't do any cleanup
    if noCleanup:
        logger.info("Skipping removal of the build user '" + rpmBuildUser + "' and its home directory at " + rpmReleaseDir + " per configuration")
        return

    # Remove the release directory
    logger.info("Removing release directory at " + rpmReleaseDir)
    shutil.rmtree(rpmReleaseDir, True)

    # Remove the temporary user and it's home directory
    logger.info("Removing the build user '" + rpmBuildUser + "' and its home directory")
    os.popen("sudo userdel --remove --force " + rpmBuildUser)
    shutil.rmtree(rpmBuildUserHome, True)

    return

# -----------------------------------------------------------------------------------
# This function reads the rpm spec file from specTemplate and stores it at
# the location indicated by specFinal while replacing the placeholders with the
# version and release information.
#
# @param specTemplate
#                    template of spec file containing placeholders
# @param specFinal
#                    final location for the processed spec file
# @param version
#                    the rpm version
# @param release
#                    the rpm release
# -----------------------------------------------------------------------------------

def prepareSpecFile(specTemplate, specFinal, version, release):

    logger.info("Adjusting version and release information in rpm spec file " + specTemplate)
    if not os.path.exists(specTemplate):
        logger.error("RPM spec template can't be found at " + str(specTemplate))
        cleanBuildEnvironment()
        sys.exit(1)

    # Parse the spec file
    f = open(specTemplate, "r")
    try:
        specFinalContents = f.read()
        logger.debug("RPM spec file at " + str(specTemplate) + " successfully read")
    except IOError:
        logger.error("RPM spec file at " + str(specTemplate) + " can't be read")
        cleanBuildEnvironment()
        sys.exit(1)
    finally:
        f.close()

    # Make the replacements
    specFinalContents = specFinalContents.replace("CHANGE_ME_VERSION", version.replace("-", "."))
    specFinalContents = specFinalContents.replace("CHANGE_ME_RELEASE", release)

    logger.info("Moving the updated rpm spec file to " + specFinal)
    # Parse the spec file
    f = open(specFinal, "w")
    try:
        specFinalContents = f.write(specFinalContents)
        logger.debug("RPM spec file at " + str(specFinal) + " successfully stored")
    except IOError:
        logger.error("Error writing RPM spec file to " + str(specFinal))
        cleanBuildEnvironment()
        sys.exit(1)
    finally:
        f.close()

    return

# -----------------------------------------------------------------------------------
# Removes everything that has "debuginfo" in its name and is inside of the specified
# directory.
#
# @param directory
#					 the directory
# -----------------------------------------------------------------------------------

def removeDebugInfo(directory):

    entries = (os.path.join(directory, fn) for fn in filter(os.path.isfile, glob.glob(directory + "/*debuginfo*")))
    for path in entries:
        logger.info("Removing " + path)
        os.remove(path)

    return

# -----------------------------------------------------------------------------------
# Remove all but a number of files.
#
# @param directory
#					 the directory
# @param size
#					 the number of artifacts to keep
# -----------------------------------------------------------------------------------

def shortenHistory(directory, size):

    # Remove everything but the last three rpms
    # get all entries in the directory w/ stats
    entries = (os.path.join(directory, fn) for fn in filter(os.path.isfile, glob.glob(directory + "/*")))
    entries = ((os.stat(path), path) for path in entries)

    # leave only regular files, insert creation date
    entries = ((stat[ST_CTIME], path) for stat, path in entries if S_ISREG(stat[ST_MODE]))

    count = 0
    for cdate, path in sorted(entries, reverse=True):
        count += 1
        if count > size:
            logger.info("Removing outdated rpm " + path)
            os.remove(path)

    return

# -----------------------------------------------------------------------------------
#
#										MAIN
#
# -----------------------------------------------------------------------------------


# -----------------------------------------------------------------------------------
# Logging setup
#
# The logger is writing to rpm-build.log in addition to stdout.
# -----------------------------------------------------------------------------------

# Define the log format
formatter = logging.Formatter('[%(levelname)s] %(message)s')

logger = logging.getLogger()
logger.setLevel(logLevel)

# Define the stdout logger
logChannel = logging.StreamHandler(sys.stdout)
logChannel.setLevel(logLevel)
logChannel.setFormatter(formatter)
logger.addHandler(logChannel)

# -----------------------------------------------------------------------------------
# Compile the list of modules based on the selected profiles.
#
# The profiles consist of the core modules which are to be included in every RPM as
# well as a list of modules related to a specific project.
# -----------------------------------------------------------------------------------

# Get the project to use for the modules to put in the RPMs

parser = argparse.ArgumentParser()
parser.add_argument("-a", "--assembly", help="The Karaf assembly to package")
parser.add_argument("-d", "--directory", help="Path to the root of the rpm directory")
parser.add_argument("-e", "--environment", help="The environment, usually one of [development, staging, production]")
parser.add_argument("-n", "--nocleanup", help="Do not remove the build artifacts upon error")
parser.add_argument("-p", "--project", help="Prefix for Maven profiles whose modules are to be included in the resulting RPM")
parser.add_argument("-w", "--workspace", help="Path to the Jenkins workspace")
args = parser.parse_args()

# Make sure all mandatory arguments have been passed

# The path to the root directory of the RPM repository is mandatory
rpmRepositoryRootDir = args.directory
if not rpmRepositoryRootDir:
    parser.error("The mandatory argument 'directory' is missing")
logger.info("The rpm root directory is " + rpmRepositoryRootDir)

# The karaf assembly is mandatory
assembly = args.assembly
if not assembly:
    parser.error("The mandatory argument 'assembly' is missing")
logger.info("Using karaf assembly " + assembly)

# The path to the workspace is mandatory
workspace = args.workspace
if not workspace:
    parser.error("The mandatory argument 'workspace' is missing")
logger.info("Using project workspace at " + workspace)

# The project profile prefix is optional
projectProfilePrefix = args.project
if projectProfilePrefix:
    logger.info("Project prefix is '" + projectProfilePrefix + "'")
else:
    logger.warn("No project selected")

# The project environment is optional
projectEnvironment = args.environment
if projectEnvironment:
    logger.info("Project environment is '" + projectEnvironment + "'")
else:
    logger.warn("No project environment selected")

# Whether to cleanup upon failure
if args.nocleanup:
    noCleanup = True

# Package name
packageName = projectProfilePrefix + "-" + applicationName

# Path to the RPM spec file
specFileName = packageName + ".spec"

# Locate the main pom
mainPom = workspace + "/pom.xml"
if not os.path.exists(mainPom):
    logger.error("Main pom can't be found at " + str(mainPom))
    cleanBuildEnvironment()
    sys.exit(1)

# Parse the pom
f = open(mainPom, "r")
try:
    mainPomXml = ETree.parse(mainPom)
    logger.debug("Main pom at " + str(mainPom) + " successfully parsed")
except IOError:
    logger.error("Main pom at " + str(mainPom) + " can't be parsed")
    cleanBuildEnvironment()
    sys.exit(1)
finally:
    f.close()

# Determine the Maven project version
projectVersion = mainPomXml.find("/{http://maven.apache.org/POM/4.0.0}version").text
logger.info("The Maven project version is " + projectVersion)

# Location of the assembly to package
assemblyDir = workspace + "/build/" + assembly + "-" + projectVersion
logger.info("The Karaf Assembly is located at " + assemblyDir)

# get major release version
# NOTE as of 2.4.x => 3.x
projectRelease = projectVersion[:1]

# The rpm version string must not contain hyphens
rpmVersion = projectVersion.replace("-", ".")


# -----------------------------------------------------------------------------------
# Create a user that is used to manage the selected build artifacts
#
# The user's home directory will be created using the process' pid and the the
# build's git hash. The same user will then be used by the following script
# (rpm-build.sh) to pick up the artifacts and include them in the RPM.
# -----------------------------------------------------------------------------------

# Determine this process' pid and the git hash
pid = os.getpid()
gitHash = os.popen("git log -1 --pretty=format:\"%ad %h\" --date=short|sed s/'[[:space:]]'/.\"$(git log --oneline|wc -l)\"git/|sed s/-//g").read()
gitHashShort = os.popen("git log -1 --pretty=format:\"%h\"").read()

# Determine the database schema's build version
dbSchemaVersion = os.popen("git log -1 --format=\"%ad %h\" --date=short -- " + dbSchemaFile + "|sed s/'[[:space:]]'/-/").read()

# Create a build user, since building the rpm is best done as a dummy user
rpmBuildUser = "pid-" + str(pid) + "-git-" + gitHashShort
rpmBuildGroup = rpmBuildUser
rpmBuildUserHome = "/var/tmp/" + rpmBuildUser
logger.info("Creating build user '" + rpmBuildUser + "' with the user's home directory located at " + rpmBuildUserHome)
os.popen("sudo useradd " + rpmBuildUser + " -d " + rpmBuildUserHome + " -m -c \"RPM build user for Matterhorn build " + gitHashShort + "\"")

# Prepare the user's home directory for the rpm build
logger.info("Preparing the rpm build environment at " + rpmBuildUserHome)
os.popen("sudo su - " + rpmBuildUser + " -c \"rpmdev-setuptree\"")

# These are the directories created by rpmdev-setuptree
rpmBuildDir = rpmBuildUserHome + "/rpmbuild"
rpmSpecDir = rpmBuildDir + "/SPECS"
rpmSourceDir = rpmBuildDir + "/SOURCES"

# -----------------------------------------------------------------------------------
# Prepare the RPM build environment.
#
# This includes creation of the necessary directories, copying of spec file and the
# contents of the future RPM.
# -----------------------------------------------------------------------------------

logger.setLevel(logging.DEBUG)

# Process the spec file and move it to the SPECS directory
rpmSpecFileTemplate = workspace + "/docs/scripts/rpm/" + specFileName
rpmSpecFile = rpmSpecDir + "/" + specFileName
prepareSpecFile(rpmSpecFileTemplate, rpmSpecFile, projectVersion, gitHash)

rpmReleaseDir = rpmBuildUserHome + "/" + packageName + "-" + rpmVersion + "-" + gitHash
rpmLibReleaseDir = rpmReleaseDir + "/lib"
rpmSourceTarball = packageName + "-" + rpmVersion + "-" + gitHash + ".tar.gz"

# Copy the static set of files to the rpm release directory
logger.info("Moving static set of rpm contents to " + rpmReleaseDir)
dir_util.copy_tree(str(assemblyDir) + "/bin", rpmReleaseDir + "/bin")
file_util.copy_file(str(assemblyDir) + "/README.md", rpmReleaseDir + "/README.md")
file_util.copy_file(str(assemblyDir) + "/LICENSE", rpmReleaseDir + "/LICENSE")
file_util.copy_file(str(assemblyDir) + "/NOTICES", rpmReleaseDir + "/NOTICES")
dir_util.copy_tree(str(assemblyDir) + "/docs/scripts/ddl", rpmReleaseDir + "/docs/ddl")
dir_util.copy_tree(str(assemblyDir) + "/docs/scripts/service", rpmReleaseDir + "/docs/service")
dir_util.copy_tree(str(assemblyDir) + "/docs/upgrade", rpmReleaseDir + "/docs/upgrade")
dir_util.mkpath(rpmReleaseDir + "/etc")
dir_util.copy_tree(str(assemblyDir) + "/lib", rpmReleaseDir + "/lib")
dir_util.copy_tree(str(assemblyDir) + "/system", rpmReleaseDir + "/system")


# Create the RPM source tarball
tarFile = rpmSourceDir + "/" + rpmSourceTarball
logger.info("Creating rpm source tarball at " + tarFile)
tar = tarfile.open(tarFile, "w:gz")
try:
    tar.add(rpmReleaseDir, arcname=os.path.basename(rpmReleaseDir))
except IOError:
    logger.error("Error creating tar file at " + tarFile)
    cleanBuildEnvironment()
    sys.exit(1)
finally:
    tar.close()

# Remove the release directory
logger.info("Removing release directory at " + rpmReleaseDir)
shutil.rmtree(rpmReleaseDir)

# Create the RPM
os.popen("sudo chown -R " + rpmBuildUser + " " + rpmBuildUserHome)
os.popen("sudo chgrp -R " + rpmBuildGroup + " " + rpmBuildUserHome)
os.popen("sudo chmod -R 777 " + rpmBuildUserHome)

try:
    logger.info("Creating the " + packageName + " rpm using " + rpmSpecFile)
    subprocess.check_call("sudo su - " + rpmBuildUser + " -c \"/usr/bin/rpmbuild -bb " + rpmSpecFile + "\"", shell=True)
except subprocess.CalledProcessError as e:
    logger.error("Creating the rpm failed with exit code " + str(e.returncode))
    cleanBuildEnvironment()
    sys.exit(1)

# -----------------------------------------------------------------------------------
# Deploying source and binary RPM to the RPM repository.
#
# The repository is located at rpmRepositoryDir
# -----------------------------------------------------------------------------------

rpmRepositoryDir = ""

# Create the path to the RPM directory
if projectProfilePrefix:
    rpmRepositoryDir = rpmRepositoryRootDir + "/" + projectProfilePrefix

rpmRepositoryDir += "/" + applicationName + "/" + projectRelease

if projectEnvironment:
    rpmRepositoryDir += "/" + projectEnvironment

if not os.path.isdir(rpmRepositoryDir):
    dir_util.mkpath(rpmRepositoryDir)
    logger.info("Creating Repo dir " + rpmRepositoryDir)

# Move the RPM (source and binary) to the
logger.info("Moving rpms to rpm repository at " + rpmRepositoryDir)
dir_util.copy_tree(rpmBuildDir + "/RPMS/x86_64", rpmRepositoryDir + "/RPMS")
dir_util.copy_tree(rpmBuildDir + "/SRPMS", rpmRepositoryDir + "/SRPMS")

# Clear debuginfo
if not rpmDebugInfo:
    logger.info("Remove debuginfo from " + rpmRepositoryDir)
    removeDebugInfo(rpmRepositoryDir + "/RPMS")
    removeDebugInfo(rpmRepositoryDir + "/SRPMS")

# Remove outdated artifacts
logger.info("Remove all rpms from " + rpmRepositoryDir + " except for the " + str(rpmHistorySize) + " newest ones")
shortenHistory(rpmRepositoryDir + "/RPMS", rpmHistorySize)
shortenHistory(rpmRepositoryDir + "/SRPMS", rpmHistorySize)

# Update the RPM repository metadata
try:
    logger.info("Updating the rpm repository's metadata")
    subprocess.check_call("sudo createrepo \"" + rpmRepositoryDir + "\"", shell=True)
except subprocess.CalledProcessError as e:
    logger.error("Updating the rpm repository's metadata failed with exit code " + str(e.returncode))
    cleanBuildEnvironment()
    sys.exit(1)

# -----------------------------------------------------------------------------------
# Cleanup
#
# Remove the build user, which will also remove the build directory.
# -----------------------------------------------------------------------------------

# Remove the temporary user and data
cleanBuildEnvironment()

# That's it!
logger.info("Creation of " + applicationName + " rpm '" + packageName + "-" + rpmVersion + "-" + gitHash + "' finished")
