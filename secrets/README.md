# Secrets

## Dependencies
Secret encryption uses `gpg` so you'll need that available on your command line.
* On macos you can install using Homebrew: `brew install gnupg`.

## How do I get secrets?
You need to run this step in two scenarios:
1. When you clone this repository.
2. Anytime anyone adds a new secret or makes any other changes
   (because your local ignored secrets file won't automatically update on `git pull`).

In either of these cases run `secrets/decrypt.sh` from the root directory.
When the script asks you for the key, look for it in the Android Team vault in 1password
and paste it in the terminal. It will decrypt the checked-in encrypted secrets file
and put the decrypted file in the expected location.

## How do I add a new secret or make other secrets changes?
Make changes to your local ignored decrypted file.
Then run `secrets/encrypt.sh` from the root directory to update the checked-in encrypted file.
When the script asks you for the key, look for it in the Android Team vault in 1password
and paste it in the terminal.

## Why does the script ask to paste the key?
The way the script is written the key won't be visible on your screen or your terminal history.

## What about CI?
The script also looks for the key in the `GPG_KEY` environment variable.
CI's usually have a way of providing secret env vars that are redacted in build logs.
So any time we setup CI we just need to provide this one secret key and it will be able
to decrypt secrets file using the same scripts developers do.