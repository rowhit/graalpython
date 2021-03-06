/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates.
 * Copyright (c) 2014, Regents of the University of California
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.graal.python.builtins.objects.set;

import static com.oracle.graal.python.nodes.SpecialMethodNames.__HASH__;
import static com.oracle.graal.python.nodes.SpecialMethodNames.__OR__;
import static com.oracle.graal.python.runtime.exception.PythonErrorType.TypeError;

import java.util.List;

import com.oracle.graal.python.builtins.Builtin;
import com.oracle.graal.python.builtins.CoreFunctions;
import com.oracle.graal.python.builtins.PythonBuiltinClassType;
import com.oracle.graal.python.builtins.PythonBuiltins;
import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.common.HashingCollectionNodes;
import com.oracle.graal.python.builtins.objects.common.HashingStorageNodes;
import com.oracle.graal.python.nodes.call.special.LookupAndCallBinaryNode;
import com.oracle.graal.python.nodes.function.PythonBuiltinBaseNode;
import com.oracle.graal.python.nodes.function.builtins.PythonBinaryBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonUnaryBuiltinNode;
import com.oracle.graal.python.runtime.exception.PythonErrorType;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ValueProfile;

@CoreFunctions(extendClasses = PythonBuiltinClassType.PSet)
public final class SetBuiltins extends PythonBuiltins {

    @Override
    protected List<? extends NodeFactory<? extends PythonBuiltinBaseNode>> getNodeFactories() {
        return SetBuiltinsFactory.getFactories();
    }

    @Builtin(name = "clear", fixedNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    public abstract static class ClearNode extends PythonUnaryBuiltinNode {

        @Specialization
        public Object clear(PSet self,
                        @Cached("createClassProfile()") ValueProfile storageProfile) {
            storageProfile.profile(self.getDictStorage()).clear();
            return PNone.NONE;
        }
    }

    @Builtin(name = "add", fixedNumOfPositionalArgs = 2)
    @GenerateNodeFactory
    public abstract static class AddNode extends PythonBinaryBuiltinNode {

        @Specialization
        public Object add(PSet self, Object o,
                        @Cached("create()") HashingCollectionNodes.SetItemNode setItemNode) {
            setItemNode.execute(self, o, PNone.NO_VALUE);
            return PNone.NONE;
        }
    }

    @Builtin(name = __HASH__, fixedNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    public abstract static class HashNode extends PythonUnaryBuiltinNode {
        @Specialization
        Object doGeneric(Object self) {
            throw raise(TypeError, "unhashable type: '%p'", self);
        }
    }

    @Builtin(name = __OR__, fixedNumOfPositionalArgs = 2)
    @GenerateNodeFactory
    public abstract static class OrNode extends PythonBinaryBuiltinNode {
        @Specialization
        Object doSet(PBaseSet self, PBaseSet other,
                        @Cached("create()") HashingStorageNodes.UnionNode unionNode) {
            return factory().createSet(unionNode.execute(self.getDictStorage(), other.getDictStorage()));
        }

        @Specialization
        Object doReverse(PBaseSet self, Object other,
                        @Cached("create(__OR__)") LookupAndCallBinaryNode callOr) {
            return callOr.executeObject(other, self);
        }
    }

    @Builtin(name = "remove", fixedNumOfPositionalArgs = 2)
    @GenerateNodeFactory
    abstract static class RemoveNode extends PythonBinaryBuiltinNode {
        @Specialization
        Object remove(PBaseSet self, Object other,
                        @Cached("create()") HashingStorageNodes.DelItemNode delItemNode) {

            if (!delItemNode.execute(self, self.getDictStorage(), other)) {
                throw raise(PythonErrorType.KeyError, "%s", other);
            }
            return PNone.NONE;
        }
    }

    @Builtin(name = "discard", fixedNumOfPositionalArgs = 2)
    @GenerateNodeFactory
    abstract static class DiscardNode extends PythonBinaryBuiltinNode {
        @Specialization
        Object discard(PBaseSet self, Object other,
                        @Cached("create()") HashingStorageNodes.DelItemNode delItemNode) {

            delItemNode.execute(self, self.getDictStorage(), other);
            return PNone.NONE;
        }
    }

}
