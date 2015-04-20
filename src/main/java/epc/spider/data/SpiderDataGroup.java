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

	private String dataId;
	private Map<String, String> attributes = new HashMap<>();
	private List<SpiderDataElement> children = new ArrayList<>();

	public static SpiderDataGroup withDataId(String dataId) {
		return new SpiderDataGroup(dataId);
	}

	private SpiderDataGroup(String dataId) {
		this.dataId = dataId;
	}

	public static SpiderDataGroup fromDataGroup(DataGroup dataGroup) {
		return new SpiderDataGroup(dataGroup);
	}

	private SpiderDataGroup(DataGroup dataGroup) {
		dataId = dataGroup.getDataId();
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
	public String getDataId() {
		return dataId;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public List<SpiderDataElement> getChildren() {
		return children;
	}

	public void addAttributeByIdWithValue(String dataId, String value) {
		attributes.put(dataId, value);
	}

	public void addChild(SpiderDataElement dataElement) {
		children.add(dataElement);
	}

	public DataGroup toDataGroup() {
		DataGroup dataGroup = DataGroup.withDataId(dataId);
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

	public SpiderDataGroup extractGroup(String groupId) {
		for (SpiderDataElement spiderDataElement : getChildren()) {
			if (spiderDataElement.getDataId().equals(groupId)) {
				return (SpiderDataGroup) spiderDataElement;
			}
		}
		throw new DataMissingException("Requested dataGroup does not exist");
	}

	public String extractAtomicValue(String atomicId) {
		for (SpiderDataElement spiderDataElement : getChildren()) {
			if (spiderDataElement.getDataId().equals(atomicId)) {
				return ((SpiderDataAtomic) spiderDataElement).getValue();
			}
		}
		throw new DataMissingException("Requested dataAtomic does not exist");
	}
}
