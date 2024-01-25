package se.uu.ub.cora.spider.extended.binary;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class BinaryProtocolsExtendedFunctionalityTest {

	private ExtendedFunctionality extFunctionality;
	private ExtendedFunctionalityData data;
	private DataRecordSpy dataRecordSpy;
	private DataGroupSpy binaryPublished;
	private DataGroupSpy adminInfoVisibilityPublished;

	@BeforeMethod
	public void beforeMethod() {
		extFunctionality = new BinaryProtocolsExtendedFunctionality();
		dataRecordSpy = new DataRecordSpy();

		data = new ExtendedFunctionalityData();

		binaryPublished = getDataGroupSpyForBinaryPublished();

	}

	private DataGroupSpy getDataGroupSpyForBinaryPublished() {
		adminInfoVisibilityPublished = new DataGroupSpy();
		adminInfoVisibilityPublished.MRV.setSpecificReturnValuesSupplier(
				"getFirstAtomicValueWithNameInData", () -> "published", "visibility");

		DataGroupSpy binaryPublished = new DataGroupSpy();
		binaryPublished.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> adminInfoVisibilityPublished, "adminInfo");

		return binaryPublished;
	}

	@Test
	public void testSetUpProtocolIfBinaryPublished() throws Exception {
		dataRecordSpy.MRV.setDefaultReturnValuesSupplier("getType", () -> "binary");
		dataRecordSpy.MRV.setDefaultReturnValuesSupplier("getDataGroup", () -> binaryPublished);
		data.dataRecord = dataRecordSpy;

		extFunctionality.useExtendedFunctionality(data);

		dataRecordSpy.MCR.assertParameters("getDataGroup", 0);

		binaryPublished.MCR.assertParameters("getFirstGroupWithNameInData", 0, "adminInfo");
		adminInfoVisibilityPublished.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0,
				"visibility");

		dataRecordSpy.MCR.assertParameters("addProtocol", 0, "iiif");
	}

	@Test
	public void testSetUpProtocolIfBinaryNotPublished() throws Exception {
		dataRecordSpy.MRV.setDefaultReturnValuesSupplier("getType", () -> "binary");
		dataRecordSpy.MRV.setDefaultReturnValuesSupplier("getDataGroup", DataGroupSpy::new);
		data.dataRecord = dataRecordSpy;

		extFunctionality.useExtendedFunctionality(data);

		dataRecordSpy.MCR.assertParameters("getDataGroup", 0);
		dataRecordSpy.MCR.assertMethodNotCalled("addProtocol");
	}

}
