package epc.spider.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import epc.metadataformat.data.DataAtomic;
import epc.metadataformat.data.DataElement;
import epc.metadataformat.data.DataGroup;

public final class SpiderDataGroup implements SpiderDataElement {

	private String nameInData;
	private Map<String, String> attributes = new HashMap<>();
	private List<SpiderDataElement> children = new ArrayList<>();

	public static SpiderDataGroup withNameInData(String nameInData) {
		return new SpiderDataGroup(nameInData);
	}

	private SpiderDataGroup(String nameInData) {
		this.nameInData = nameInData;
	}

	public static SpiderDataGroup fromDataGroup(DataGroup dataGroup) {
		return new SpiderDataGroup(dataGroup);
	}

	private SpiderDataGroup(DataGroup dataGroup) {
		nameInData = dataGroup.getNameInData();
		attributes.putAll(dataGroup.getAttributes());
		convertAndSetChildren(dataGroup);
	}

	private void convertAndSetChildren(DataGroup dataGroup) {
		for (DataElement dataElement : dataGroup.getChildren()) {
			children.add(convertToSpiderEquivalentDataClass(dataElement));
		}
	}

	private SpiderDataElement convertToSpiderEquivalentDataClass(DataElement dataElement) {
		if (dataElement instanceof DataGroup) {
			return new SpiderDataGroup((DataGroup) dataElement);
		}
		return SpiderDataAtomic.fromDataAtomic((DataAtomic) dataElement);
	}

	@Override
	public String getNameInData() {
		return nameInData;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public List<SpiderDataElement> getChildren() {
		return children;
	}

	public void addAttributeByIdWithValue(String nameInData, String value) {
		attributes.put(nameInData, value);
	}

	public void addChild(SpiderDataElement dataElement) {
		children.add(dataElement);
	}

	public DataGroup toDataGroup() {
		DataGroup dataGroup = DataGroup.withNameInData(nameInData);
		addAttributesToDataGroup(dataGroup);

		addChildrenToDataGroup(dataGroup);

		return dataGroup;
	}

	private void addAttributesToDataGroup(DataGroup dataGroup) {
		for (Entry<String, String> entry : attributes.entrySet()) {
			dataGroup.addAttributeByIdWithValue(entry.getKey(), entry.getValue());
		}
	}

	private void addChildrenToDataGroup(DataGroup dataGroup) {
		for (SpiderDataElement child : children) {
			dataGroup.addChild(convertToCorrectDataElement(child));
		}
	}

	private DataElement convertToCorrectDataElement(SpiderDataElement child) {
		if (child instanceof SpiderDataGroup) {
			return ((SpiderDataGroup) child).toDataGroup();
		}
		return ((SpiderDataAtomic) child).toDataAtomic();
	}

	public boolean containsChildWithNameInData(String nameInData) {
		for (SpiderDataElement dataElement : children) {
			if (dataElement.getNameInData().equals(nameInData)) {
				return true;
			}
		}
		return false;
	}

	public SpiderDataGroup extractGroup(String groupId) {
		for (SpiderDataElement spiderDataElement : getChildren()) {
			if (spiderDataElement.getNameInData().equals(groupId)) {
				return (SpiderDataGroup) spiderDataElement;
			}
		}
		throw new DataMissingException("Requested dataGroup " + groupId + " does not exist");
	}

	public String extractAtomicValue(String atomicId) {
		for (SpiderDataElement spiderDataElement : getChildren()) {
			if (spiderDataElement.getNameInData().equals(atomicId)) {
				return ((SpiderDataAtomic) spiderDataElement).getValue();
			}
		}
		throw new DataMissingException("Requested dataAtomic " + atomicId + " does not exist");
	}
}
