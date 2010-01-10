/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.context;

import javolution.util.FastMap;

import org.ofbiz.context.PathNode.BranchNode;
import org.ofbiz.context.PathNode.SubstitutionNode;
import org.ofbiz.context.PathNode.WildCardNode;

public class PermissionTreeBuilder implements PathNodeVisitor {

    protected ArtifactPath artifactPath;
    protected OFBizPermission permission;

    public void buildPermissionTree(PathNode node, ArtifactPath artifactPath, OFBizPermission permission) {
        this.artifactPath = artifactPath;
        this.permission = permission;
        node.accept(this);
    }

    protected void setChildNodePermissions(PathNode node, String key) {
        if (node.childNodes == null) {
            node.childNodes = FastMap.newInstance();
        }
        key = key.toUpperCase();
        PathNode childNode = node.childNodes.get(key);
        if (childNode == null) {
            childNode = PathNode.getInstance(this.artifactPath);
            node.childNodes.put(key, childNode);
        }
        childNode.accept(this);
    }

    @Override
    public void visit(BranchNode node) {
        if (!this.artifactPath.hasNext()) {
            if (node.permission == null) {
                node.permission = this.permission;
            } else {
                node.permission.accumulatePermissions(this.permission);
            }
            return;
        }
        String key = this.artifactPath.next();
        if (PathNode.SUBSTITUTION_CHARACTER.equals(key)) {
            if (node.substitutionNode == null) {
                node.substitutionNode = new SubstitutionNode();
            }
            node.substitutionNode.accept(this);
            return;
        }
        if (PathNode.WILDCARD_CHARACTER.equals(key)) {
            if (node.wildCardNode == null) {
                node.wildCardNode = new WildCardNode();
            }
            node.wildCardNode.accept(this);
            return;
        }
        this.setChildNodePermissions(node, key);
    }

    @Override
    public void visit(SubstitutionNode node) {
        if (this.artifactPath.hasNext()) {
            this.setChildNodePermissions(node, this.artifactPath.next());
        }
    }

    @Override
    public void visit(WildCardNode node) {
        if (this.artifactPath.hasNext()) {
            this.artifactPath.next();
            this.setChildNodePermissions(node, this.artifactPath.getCurrentPath());
        }
    }
}
