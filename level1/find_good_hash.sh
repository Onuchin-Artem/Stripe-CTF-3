tree=$1
parent=$2
timestamp=$3
difficulty=$4


counter=0

while let counter=counter+1; do

	body="tree $tree
parent $parent
author CTF user <me@example.com> $timestamp +0000
committer CTF user <me@example.com> $timestamp +0000

Give me a Gitcoin

$counter"

sha1=$(git hash-object -t commit --stdin <<< "$body")

if [ "$sha1" "<" "$difficulty" ]; then
fi
done