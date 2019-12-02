#
# Copyright 2019-present MongoDB, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""This file finds and prints or makes and prints a data key used for
encrpytion."""

from helpers import read_master_key, CsfleHelper


def main():

    local_master_key = read_master_key()

    kms_providers = {
        "local": {
            "key": local_master_key,
        },
    }

    csfle_helper = CsfleHelper(kms_providers=kms_providers)
    data_key = csfle_helper.find_or_create_data_key()

    print("Base64 data key. Copy and paste this into app.py\t", data_key)


if __name__ == "__main__":
    main()
