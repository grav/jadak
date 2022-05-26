#!/bin/bash 

set -eux pipefail

classpath="$(clojure -A:nbb -Spath -Sdeps '{:aliases {:nbb {:replace-deps {com.github.grav/jadak {:git/sha "4ccb6f2b75ea06f6e99e265830a79153393895eb"} com.github.juxt/bidi {:git/sha "d94316235c4e59d7c44fca9fcc489b386f526bcc"}}}}}')"

# requires node
npx nbb --classpath "$classpath" -e "(require '[jadak.jadak :as jadak])"

