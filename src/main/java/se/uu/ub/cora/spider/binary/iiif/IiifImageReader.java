package se.uu.ub.cora.spider.binary.iiif;

import se.uu.ub.cora.spider.data.ResourceInputStream;

public interface IiifImageReader {

	/**
	 * readImage read an image using a IIIF protocol. All parameters are specified in the IIIF
	 * standard. @see <a href="https://iiif.io/api/image/">IIIF Image Api</a> Further desciption of
	 * the parameters can be found in the linked webpage.
	 * 
	 * @param identifier
	 *            The id of the record to read the image from.
	 * @param region
	 *            The rectangular portion of the underlying image
	 * @param size
	 *            The size parameter specifies the dimensions to which the extracted region is to be
	 *            scaled.
	 * @param rotation
	 *            The rotation parameter specifies mirroring and rotation.
	 * @param quality
	 *            The quality parameter determines whether the image is delivered in color,
	 *            grayscale or black and white.
	 * @param format
	 *            The format of the returned image is expressed as a suffix, mirroring common
	 *            filename extensions, at the end of the URI.
	 * @return A {@link ResourceInputStream} containing information about the requested image.
	 * @throws NF
	 */
	ResourceInputStream readImage(String identifier, String region, String size, String rotation,
			String quality, String format);

}
