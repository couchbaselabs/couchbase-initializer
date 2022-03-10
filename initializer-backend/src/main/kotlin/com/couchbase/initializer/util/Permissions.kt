/*
 * Copyright 2022 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.initializer.util

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions

val File.permissions: Set<PosixFilePermission>?
    get() {
        return try {
            Files.getPosixFilePermissions(toPath())
        } catch (_: UnsupportedOperationException) {
            null
        }
    }

val PosixFilePermission.bitmask: Int
    get() = when (this) {
        PosixFilePermission.OTHERS_EXECUTE -> 1 shl 0
        PosixFilePermission.OTHERS_WRITE -> 1 shl 1
        PosixFilePermission.OTHERS_READ -> 1 shl 2
        PosixFilePermission.GROUP_EXECUTE -> 1 shl 3
        PosixFilePermission.GROUP_WRITE -> 1 shl 4
        PosixFilePermission.GROUP_READ -> 1 shl 5
        PosixFilePermission.OWNER_EXECUTE -> 1 shl 6
        PosixFilePermission.OWNER_WRITE -> 1 shl 7
        PosixFilePermission.OWNER_READ -> 1 shl 8
    }

val rwxr_xr_x = PosixFilePermissions.fromString("rwxr-xr-x").toInt()

fun Set<PosixFilePermission>.toInt(): Int {
    var mode = 0
    forEach { perm -> mode = mode or perm.bitmask }
    return mode
}

val Set<PosixFilePermission>.executable: Boolean
    get() = contains(PosixFilePermission.OWNER_EXECUTE)
