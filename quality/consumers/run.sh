#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
root_dir="$(cd "$script_dir/../.." && pwd -P)"

cleanup_repository=false
if [[ -n "${COLOTOK_CONSUMER_REPOSITORY:-}" ]]; then
    mkdir -p "$COLOTOK_CONSUMER_REPOSITORY"
    repository="$(cd "$COLOTOK_CONSUMER_REPOSITORY" && pwd -P)"
else
    repository="$(mktemp -d "${TMPDIR:-/tmp}/colotok-consumer-m2.XXXXXX")"
    cleanup_repository=true
fi

default_maven_repository="$(cd "$HOME" && pwd -P)/.m2/repository"
if [[ "$repository" == "$default_maven_repository" ]]; then
    echo "Refusing to use the default Maven local repository: $repository" >&2
    exit 2
fi

cleanup() {
    if [[ "$cleanup_repository" == true ]]; then
        rm -rf "$repository"
    fi
}
trap cleanup EXIT

colotok_version="$("$root_dir/gradlew" -q :colotok:properties --no-configuration-cache \
    | awk '/^version:/ { print $2; exit }')"
if [[ -z "$colotok_version" ]]; then
    echo "Could not determine the Colotok publication version" >&2
    exit 2
fi

"$root_dir/gradlew" \
    :colotok:publishKotlinMultiplatformPublicationToMavenLocal \
    :colotok:publishJvmPublicationToMavenLocal \
    :colotok-slf4j:publishMavenPublicationToMavenLocal \
    :colotok-slf4j2:publishMavenPublicationToMavenLocal \
    "-Dmaven.repo.local=$repository" \
    -Pcolotok.skipPublicationSigning=true \
    --rerun-tasks \
    --no-configuration-cache \
    --max-workers=1

for consumer in slf4j1 slf4j2; do
    "$root_dir/gradlew" \
        -p "$script_dir/$consumer" \
        consumerSmoke \
        "-PcolotokRepository=$repository" \
        "-PcolotokVersion=$colotok_version" \
        --refresh-dependencies \
        --rerun-tasks \
        --no-configuration-cache \
        --max-workers=1
done
