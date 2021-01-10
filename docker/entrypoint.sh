#!/bin/bash
USAGE="$0"' PLAY_RUN_ARGS
Provides default arguments for the Play entrypoint. All `PLAY_RUN_ARGS` are
appended to the play entrypoint.

For example, you can change the port using
'"$0"' -Dhttp.port=9999'

function msg() { echo "[RAPPELLE_DEV ENTRYPOINT]" "$@" ; }

# getopt
SHORT='h:'
LONG='help:'
OPTS="$(getopt --options $SHORT --long $LONG --name "$0" -- "$@")"
! [ "$?" = 0 ] && echo "$USAGE" 1>&2 && exit 1
eval set -- "$OPTS"

# Parses params
while [[ "$#" -gt 0 ]]
do
    key="$1"
    case "$key" in
        -h|--help)
            echo "$USAGE"
            exit 0
            ;;
        --)
            shift
            break
            ;;
        *)
            echo "$USAGE" 1>&2
            exit 1
    esac
done

# Common options passed to the application
cmd=(
    '/apps/rappelle/run'
    '-Dhttp.port=8000'
    '-Dplay.evolutions.db.default.autoApply=true'
    '-Dpidfile.path=/dev/null'
)

# If we find a custom application config mounted at /apps/rappelle/application.conf, use it
if [ -r /apps/rappelle/application.conf ]
then
    msg "Found config file /apps/rappelle/application.conf"
    cmd+=( '-Dconfig.file=/apps/rappelle/application.conf' )

# If we didn't find a custom, use a default config
else
    msg "Using default configuration application.prod.conf"
    cmd+=( '-Dconfig.resource=application.prod.conf' )
fi

# Transparently passes any extra args
cmd+=( "$@" )

# Executes
msg "Executing: ${cmd[@]}"
"${cmd[@]}"
