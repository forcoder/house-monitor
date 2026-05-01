#!/usr/bin/env sh

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME

# Resolve links: $0 may be a link
app_path="$0"

# Need this for daisy-chained symlinks.
while
    APP_HOME="${app_path%"${app_path##*/}"}"
    case "$APP_HOME" in
        /*) break ;;
        *) APP_HOME="`cd "$APP_HOME" && pwd`" ;;
    esac
    test -x "$APP_HOME"/"${0##*/}" || break
do
    ls=$(ls -ld "$app_path")
    link=${ls#*' -> '}
    if test "$link" != "$ls" ; then
        app_path="$link"
    else
        break
    fi
done

# This is normally unused
# APP_BASE_NAME=${0##*/}
# APP_HOME=$APP_HOME/..

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
defaultJvmOpts=""

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

located=false

warn ( ) {
    echo "$*"
}

die ( ) {
    echo
    echo "ERROR: $*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false
darwin=false
mingw=false
case "`uname`" in
    CYGWIN* )
        cygwin=true
        ;;
    Darwin* )
        darwin=true
        ;;
    MINGW* )
        mingw=true
        ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine the Java command to use to start the JVM.
if test -n "$JAVA_HOME" ; then
    if test -x "$JAVA_HOME"/jre/sh/java ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME"/jre/sh/java
    else
        JAVACMD="$JAVA_HOME"/bin/java
    fi
    if test ! -x "$JAVACMD" ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Increase the maximum file descriptors if we can.
if test "$cygwin" = "true" -o "$darwin" = "true" ; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if test "$MAX_FD_LIMIT" != "unlimited" ; then
        # bash doesn't have max_fd, so don't check for the limit
        if test -n "$MAX_FD" && test "$MAX_FD" != "maximum" && test "$MAX_FD" != "max" ; then
            # use the minimum of MAX_FD and MAX_FD_LIMIT, since ulimit can only decrease
            if test "$MAX_FD" -lt "$MAX_FD_LIMIT" ; then
                MAX_FD="$MAX_FD"
            else
                MAX_FD="$MAX_FD_LIMIT"
            fi
        else
            MAX_FD="$MAX_FD_LIMIT"
        fi
        ulimit -n "$MAX_FD"
        if test "$?" != "0" ; then
            warn "Could not set maximum file descriptor limit: $MAX_FD"
        fi
    fi
fi

# For Darwin, add options to specify how the application appears in the dock
if test "$darwin" = "true" ; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=Gradle\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""
fi

# For Cygwin, switch paths to Windows format before running java
if test "$cygwin" = "true" ; then
    APP_HOME="`cygpath --path --mixed "$APP_HOME"`"
    CLASSPATH="`cygpath --path --mixed "$CLASSPATH"`"
    JAVACMD="`cygpath --unix "$JAVACMD"`"

    # We build the pattern for arguments to be converted via cygpath
    ROOTDIRSRAW=`find -L "$APP_HOME" -maxdepth 1 -type d 2>/dev/null | tr "\n" "|"`
    SEP=""
    for file in `ls -1 "$APP_HOME"/gradle/wrapper/*.jar`; do
        ROOTDIRSRAW="$SEP`cygpath --path --mixed "$file"`|$ROOTDIRSRAW"
        SEP="|"
    done
    ROOTDIRSRAW="$ROOTDIRSRAW|"
    OURCYGPATTERN="(^($ROOTDIRSRAW))"
    # Add a user-defined pattern to the mix
    if test "$GRADLE_CYGPATTERN" != "" ; then
        OURCYGPATTERN="$OURCYGPATTERN|($GRADLE_CYGPATTERN)"
    fi
    # Now convert the arguments - kludge to limit ourselves to /bin/sh
    i=0
    for arg in "$@" ; do
        CHECK=`echo "$arg"| sed -n "s@$OURCYGPATTERN@@p"`
        if test "$CHECK" != "$arg" ; then
            # Remove the pattern
            arg="`echo "$arg" | sed -e "s@$OURCYGPATTERN@@g"`"
            # Convert the path to Windows format
            arg="`cygpath --path --mixed "$arg"`"
        fi
        eval "args[$i]="$arg\""
        i=`expr "$i" + 1`
    done
    case "$i" in
        0) ;;
        1) set -- "${args[0]}" ;;
        2) set -- "${args[0]}" "${args[1]}" ;;
        3) set -- "${args[0]}" "${args[1]}" "${args[2]}" ;;
        4) set -- "${args[0]}" "${args[1]}" "${args[2]}" "${args[3]}" ;;
        5) set -- "${args[0]}" "${args[1]}" "${args[2]}" "${args[3]}" "${args[4]}" ;;
        6) set -- "${args[0]}" "${args[1]}" "${args[2]}" "${args[3]}" "${args[4]}" "${args[5]}" ;;
        7) set -- "${args[0]}" "${args[1]}" "${args[2]}" "${args[3]}" "${args[4]}" "${args[5]}" "${args[6]}" ;;
        8) set -- "${args[0]}" "${args[1]}" "${args[2]}" "${args[3]}" "${args[4]}" "${args[5]}" "${args[6]}" "${args[7]}" ;;
        9) set -- "${args[0]}" "${args[1]}" "${args[2]}" "${args[3]}" "${args[4]}" "${args[5]}" "${args[6]}" "${args[7]}" "${args[8]}" ;;
    esac
fi

# Split up the GRADLE_OPTS into options to be read via the 'args' array
i=0
temp="$IFS"
IFS=$'\n\r'
for each in `echo "$GRADLE_OPTS" | sed -e 's/ -D/\n-D/g'` ; do
    eval "args[$i]=\${each}"
    i=`expr "$i" + 1`
done
IFS="$temp"

# Collect all arguments for the java command, stacking in reverse of:
#   * args from the Java command line (e.g. environment variable)
#   * the main class name
#   * program args
#   * the --module-path argument, if present
#   * default jvm options and main class parameters
#   * JAVA_OPTS
#   * defaultJvmOpts

# For Cygwin, switch paths to Windows format before running java
if test "$cygwin" = "true" ; then
    # Collect all arguments for the java command, stacking in reverse of:
    #   * args from the Java command line (e.g. environment variable)
    #   * the main class name
    #   * program args
    #   * the --module-path argument, if present
    #   * default jvm options and main class parameters
    #   * JAVA_OPTS
    #   * defaultJvmOpts

    # For Cygwin, switch paths to Windows format before running java
    APP_HOME="`cygpath --path --mixed "$APP_HOME"`"
    CLASSPATH="`cygpath --path --mixed "$CLASSPATH"`"
    JAVACMD="`cygpath --unix "$JAVACMD"`"

    # We build the pattern for arguments to be converted via cygpath
    ROOTDIRSRAW=`find -L "$APP_HOME" -maxdepth 1 -type d 2>/dev/null | tr "\n" "|"`
    SEP=""
    for file in `ls -1 "$APP_HOME"/gradle/wrapper/*.jar`; do
        ROOTDIRSRAW="$SEP`cygpath --path --mixed "$file"`|$ROOTDIRSRAW"
        SEP="|"
    done
    ROOTDIRSRAW="$ROOTDIRSRAW|"
    OURCYGPATTERN="(^($ROOTDIRSRAW))"
    # Add a user-defined pattern to the mix
    if test "$GRADLE_CYGPATTERN" != "" ; then
        OURCYGPATTERN="$OURCYGPATTERN|($GRADLE_CYGPATTERN)"
    fi
    # Now convert the arguments - kludge to limit ourselves to /bin/sh
    i=0
    for arg in "$@" ; do
        CHECK=`echo "$arg"| sed -n "s@$OURCYGPATTERN@@p"`
        if test "$CHECK" != "$arg" ; then
            # Remove the pattern
            arg="`echo "$arg" | sed -e "s@$OURCYGPATTERN@@g"`"
            # Convert the path to Windows format
            arg="`cygpath --path --mixed "$arg"`"
        fi
        eval "args[$i]="$arg\""
        i=`expr "$i" + 1`
    done
    case "$i" in
        0) ;;
        1) set -- "${args[0]}" ;;
        2) set -- "${args[0]}" "${args[1]}" ;;
        3) set -- "${args[0]}" "${args[1]}" "${args[2]}" ;;
        4) set -- "${args[0]}" "${args[1]}" "${args[2]}" "${args[3]}" ;;
        5) set -- "${args[0]}" "${args[1]}" "${args[2]}" "${args[3]}" "${args[4]}" ;;
        6) set -- "${args[0]}" "${args[1]}" "${args[2]}" "${args[3]}" "${args[4]}" "${args[5]}" ;;
        7) set -- "${args[0]}" "${args[1]}" "${args[2]}" "${args[3]}" "${args[4]}" "${args[5]}" "${args[6]}" ;;
        8) set -- "${args[0]}" "${args[1]}" "${args[2]}" "${args[3]}" "${args[4]}" "${args[5]}" "${args[6]}" "${args[7]}" ;;
        9) set -- "${args[0]}" "${args[1]}" "${args[2]}" "${args[3]}" "${args[4]}" "${args[5]}" "${args[6]}" "${args[7]}" "${args[8]}" ;;
    esac
fi

# Escape application args
if test "$cygwin" = "true" -o "$mingw" = "true" ; then
    for arg in "$@" ; do
        eval "_args="$arg\""
        case "$_" in
            *'\'*|*' '*|*'"'*|*'*'*|*'?'*|*'<'*|*'>'*|*'|'*)
                _args="`echo "$_" | sed -e 's/\\\\/\\\\\\\\/g' -e 's/"/\\\\\\\\"/g'`"
                eval "args[$i]="$_"""
                ;;
            *)
                args[$i]="$_"
                ;;
        esac
        i=`expr "$i" + 1`
    done
    case "$i" in
        0) ;;
        1) set -- "${args[0]}" ;;
        2) set -- "${args[0]}" "${args[1]}" ;;
        3) set -- "${args[0]}" "${args[1]}" "${args[2]}" ;;
        4) set -- "${args[0]}" "${args[1]}" "${args[2]}" "${args[3]}" ;;
        5) set -- "${args[0]}" "${args[1]}" "${args[2]}" "${args[3]}" "${args[4]}" ;;
        6) set -- "${args[0]}" "${args[1]}" "${args[2]}" "${args[3]}" "${args[4]}" "${args[5]}" ;;
        7) set -- "${args[0]}" "${args[1]}" "${args[2]}" "${args[3]}" "${args[4]}" "${args[5]}" "${args[6]}" ;;
        8) set -- "${args[0]}" "${args[1]}" "${args[2]}" "${args[3]}" "${args[4]}" "${args[5]}" "${args[6]}" "${args[7]}" ;;
        9) set -- "${args[0]}" "${args[1]}" "${args[2]}" "${args[3]}" "${args[4]}" "${args[5]}" "${args[6]}" "${args[7]}" "${args[8]}" ;;
    esac
fi

# Configure the module path if necessary
MODULEPATH=""
if test -f "$APP_HOME"/gradle/wrapper/gradle-wrapper.properties; then
    MODULEPATH="$(grep "^distributionUrl" "$APP_HOME"/gradle/wrapper/gradle-wrapper.properties | sed 's/^distributionUrl=//' | sed 's/.*\/gradle-\(.*\)-bin\\.zip/\1/')"
    MODULEPATH="$APP_HOME/gradle/wrapper/gradle-$MODULEPATH.jar"
fi

# JVM parameters
JVM_PARAMS=""
if test -n "$MODULEPATH"; then
    JVM_PARAMS="$JVM_PARAMS --module-path \"$MODULEPATH\""
fi

# Collect all arguments for the java command, stacking in reverse of:
#   * args from the Java command line (e.g. environment variable)
#   * the main class name
#   * program args
#   * the --module-path argument, if present
#   * default jvm options and main class parameters
#   * JAVA_OPTS
#   * defaultJvmOpts

exec "$JAVACMD" "$JVM_PARAMS" "$JAVA_OPTS" "$defaultJvmOpts" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"