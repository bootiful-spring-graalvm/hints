#!/usr/bin/env bash

export BW_SESSION=${BW_SESSION:-`bw unlock --raw` }

export JRELEASER_GPG_PASSPHRASE=$( bw get item 'joshlong.com-maven-gpg' |  jq -r '.fields[] | select(.name == "gpg-passphrase") | .value' )
export JRELEASER_GITHUB_TOKEN=$( bw get item 'joshlong.com-maven-gpg' |  jq -r '.fields[] | select(.name == "github-pat") | .value' )
export JRELEASER_MAVENCENTRAL_USERNAME=$( bw get item 'joshlong.com-maven-gpg' |  jq -r '.fields[] | select(.name == "maven-central-username") | .value' )
export JRELEASER_MAVENCENTRAL_PASSWORD=$( bw get item 'joshlong.com-maven-gpg' |  jq -r '.fields[] | select(.name == "maven-central-token") | .value' )
mkdir -p $HOME/.jreleaser

# you'll need the key id. do gpg --list-keys and note the key id. then:
# get the value of the gpg public key: gpg --export --armor <key-id> > public-key.
# take that public-key file, cat it, base64 encode it, and put it in bitwarden under the joshlong.com-maven-gpg field called gpg-public-key
bw get item 'joshlong.com-maven-gpg' |  jq -r '.fields[] | select(.name == "gpg-public-key") | .value'  | base64 -d > $HOME/.jreleaser/public

# get the value of the gpg private key: gpg --export-secret-keys --armor <key-id> > private-key
# take that private-key file, cat it, base64 encode it, and put it in bitwarden under the joshlong.com-maven-gpg field called gpg-private-key
bw get item 'joshlong.com-maven-gpg' |  jq -r '.fields[] | select(.name == "gpg-private-key") | .value' | base64 -d > $HOME/.jreleaser/private

## STAGING
echo "setting release version..."
mvn build-helper:parse-version versions:set \
  -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}
RELEASE_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

echo "the release version is $RELEASE_VERSION "
echo "staging..."
mvn versions:commit                           # accept the release version
mvn -Ppublish clean deploy
git commit -am "releasing ${RELEASE_VERSION}" # release the main version

## RELEASE
echo "releasing..."
mvn -Ppublish jreleaser:release -N -pl :easy-spring-batch-remotechunking-spring-boot-starter

# clean up the mess we made.
rm -rf $HOME/.jreleaser/{private,public}
ls -la $HOME/.jreleaser/private && echo "the private file - $HOME/.jreleaser/private has not been deleted. delete it."

## INCREMENT VERSION NUMBER FOR THE NEXT SNAPSHOT.
mvn build-helper:parse-version versions:set \
  -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.nextIncrementalVersion}-SNAPSHOT
echo "the next snapshot version is $(mvn help:evaluate -Dexpression=project.version -q -DforceStdout) "
mvn versions:commit
SNAPSHOT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
git commit -am "moving to $SNAPSHOT_VERSION"
git push