/*******************************************************************************
 * Copyright 2012 University of Southern California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * This code was developed by the Information Integration Group as part 
 * of the Karma project at the Information Sciences Institute of the 
 * University of Southern California.  For more information, publications, 
 * and related projects, please see: http://www.isi.edu/integration
 ******************************************************************************/
package edu.isi.karma.rep;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author szekely
 * 
 */
public class HTable extends RepEntity {

	// The name of the column to use to collect single values when a column has
	// both values and nested tables.
	static final String ORPHAN_COLUMN_NAME = "orphan";

	// My name.
	private String tableName;

	// My columns: map of HNodeId to HNode
	private final Map<String, HNode> nodes = new HashMap<String, HNode>();

	private ArrayList<String> orderedNodeIds = new ArrayList<String>();

	// mariam
	/**
	 * the HNode that contains this table (useful for backwards traversing)
	 */
	private HNode parentHNode = null;

	public HTable(String id, String tableName) {
		super(id);
		this.tableName = tableName;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public Collection<HNode> getHNodes() {
		return nodes.values();
	}

	public Collection<String> getHNodeIds() {
		return nodes.keySet();
	}

	public boolean contains(HNode hNode) {
		return nodes.containsKey(hNode.getId());
	}

	public HNode getHNode(String hNodeId) {
		return nodes.get(hNodeId);
	}

	public HNode getHNodeFromColumnName(String columnName) {
		for (HNode n : nodes.values()) {
			if (columnName.equals(n.getColumnName())) {
				return n;
			}
		}
		return null;
	}

	// mariam
	/**
	 * Returns the HNode that contains this table.
	 * 
	 * @return the HNode that contains this table.
	 */
	public HNode getParentHNode() {
		return parentHNode;
	}

	/**
	 * Returns the HNodeId for the first HNode with the given columnName.
	 * 
	 * @param columnName
	 * @return the HNodeId given a columnName. Should be used only with
	 *         worksheets that do not contain nested tables.
	 */
	public String getHNodeIdFromColumnName(String columnName) {
		for (Map.Entry<String, HNode> n : nodes.entrySet()) {
			if (columnName.equals(n.getValue().getColumnName())) {
				return n.getKey();
			}
		}
		return null;
	}

	/**
	 * Returns true if this table contains nested tables, false otherwise.
	 * 
	 * @return true if this table contains nested tables, false otherwise.
	 */
	public boolean hasNestedTables() {
		for (HNode n : getHNodes()) {
			if (n.hasNestedTable())
				return true;
		}
		return false;
	}

	// ////////////////////////////////////////////

	public HNode addHNode(String columnName, Worksheet worksheet,
			RepFactory factory) {
		return addHNode(columnName, false, worksheet, factory);
	}

	public HNode addHNode(String columnName, boolean automaticallyAdded,
			Worksheet worksheet, RepFactory factory) {
		HNode hn = factory.createHNode(id, columnName, automaticallyAdded);
		nodes.put(hn.getId(), hn);
		orderedNodeIds.add(hn.getId());
		worksheet.addNodeToDataTable(hn, factory);
		return hn;
	}

	public List<HNode> getSortedHNodes() {
		List<HNode> allHNodes = new LinkedList<HNode>();
		for (String hNodeId : orderedNodeIds) {
			allHNodes.add(nodes.get(hNodeId));
		}
		return allHNodes;
	}

	public void getSortedLeafHNodes(List<HNode> sortedLeafHNodes) {
		for (String hNodeId : orderedNodeIds) {
			HNode node = nodes.get(hNodeId);
			if (node.hasNestedTable()) {
				node.getNestedTable().getSortedLeafHNodes(sortedLeafHNodes);
			} else {
				sortedLeafHNodes.add(node);
			}
		}
	}

	public void addNewHNodeAfter(String hNodeId, RepFactory factory,
			String columnName, Worksheet worksheet) {
		HNode hNode = getHNode(hNodeId);
		if (hNode == null) {
			for (HNode node : nodes.values()) {
				if (node.hasNestedTable()) {
					node.getNestedTable().addNewHNodeAfter(hNodeId, factory,
							columnName, worksheet);
				}
			}
		} else {
			HNode newNode = factory.createHNode(getId(), columnName, false);
			nodes.put(newNode.getId(), newNode);
			int index = orderedNodeIds.indexOf(hNodeId);

			if (index == orderedNodeIds.size() - 1)
				orderedNodeIds.add(newNode.getId());
			else
				orderedNodeIds.add(index + 1, newNode.getId());
			worksheet.addNodeToDataTable(newNode, factory);
		}
	}

	/**
	 * Returns ordered nodeIds.
	 * 
	 * @return ordered nodeIds.
	 * @author mariam
	 */
	public ArrayList<String> getOrderedNodeIds() {
		return orderedNodeIds;
	}

	@Override
	public void prettyPrint(String prefix, PrintWriter pw, RepFactory factory) {
		pw.print(prefix);
		pw.print("Headers/" + id + ": ");
		pw.println(tableName);

		for (HNode hn : getSortedHNodes()) {
			hn.prettyPrint(prefix, pw, factory);
		}
	}

	public List<HNodePath> getAllPaths() {
		List<HNodePath> x = new LinkedList<HNodePath>();
		for (HNode hn : getSortedHNodes()) {
			x.add(new HNodePath(hn));
		}
		return expandPaths(x);
	}

	private List<HNodePath> expandPaths(List<HNodePath> paths) {
		List<HNodePath> x = new LinkedList<HNodePath>();
		for (HNodePath p : paths) {
			if (p.getLeaf().getNestedTable() != null) {
				HTable nestedHTable = p.getLeaf().getNestedTable();
				for (HNodePath nestedP : nestedHTable.getAllPaths()) {
					x.add(HNodePath.concatenate(p, nestedP));
				}
			} else {
				x.add(p);
			}
		}
		return x;
	}

	/**
	 * Sets the parent HNode.
	 * 
	 * @param hNode
	 * @author mariam
	 */
	public void setParentHNode(HNode hNode) {
		parentHNode = hNode;
	}

	/**
	 * @param orphanColumnName
	 * @param worksheet
	 * @param factory
	 * @return the added column. If an automatically generated column with the
	 *         desired name already exists, we return it. Otherwise we create a
	 *         new one. We make sure that the new column does not conflict with
	 *         an existing column.
	 */
	HNode addAutomaticallyGeneratedColumn(String orphanColumnName,
			Worksheet worksheet, RepFactory factory) {
		HNode orphanHNode = getHNodeFromColumnName(HTable.ORPHAN_COLUMN_NAME);
		if (orphanHNode == null || !orphanHNode.isAutomaticallyAdded()) {
			// Create the new column
			orphanHNode = addHNode(
					getAutomaticallyAddedColumnName(HTable.ORPHAN_COLUMN_NAME),
					true, worksheet, factory);
		}

		return orphanHNode;
	}

	/**
	 * When we automatically add a new column, we must make sure that it's name
	 * does not conflict with a column that was not added automatically. We
	 * append underscores until the name does not conflict.
	 * 
	 * @param columnName
	 *            the name we would like to use.
	 * @return a column name that does not conflict with a source column.
	 */
	String getAutomaticallyAddedColumnName(String columnName) {
		HNode hn = getHNodeFromColumnName(columnName);
		String name = columnName;
		while (hn != null && !hn.isAutomaticallyAdded()) {
			name = "_" + name + "_";
			hn = getHNodeFromColumnName(name);
		}
		return name;
	}
}
