/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.api.helpers;

import org.apache.commons.io.FilenameUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * The ContentTypeHelper class is an utility used to map file extensions to mime types.
 */
public class ContentTypeHelper {

    /** The Constant TEXT_X_VCARD. */
    public static final String TEXT_X_VCARD = "text/x-vcard";

    /** The Constant TEXT_X_COMPONENT. */
    public static final String TEXT_X_COMPONENT = "text/x-component";

    /** The Constant TEXT_IULS. */
    public static final String TEXT_IULS = "text/iuls";

    /** The Constant TEXT_DELIMITER_SEPARATED_VALUES. */
    public static final String TEXT_DELIMITER_SEPARATED_VALUES = "text/delimiter-separated-values";

    /** The Constant TEXT_TAB_SEPARATED_VALUES. */
    public static final String TEXT_TAB_SEPARATED_VALUES = "text/tab-separated-values";

    /** The Constant APPLICATION_X_SH. */
    public static final String APPLICATION_X_SH = "application/x-sh";

    /** The Constant APPLICATION_RTF. */
    public static final String APPLICATION_RTF = "application/rtf";

    /** The Constant TEXT_RICHTEXT. */
    public static final String TEXT_RICHTEXT = "text/richtext";

    /** The Constant TEXT_SCRIPTLET. */
    public static final String TEXT_SCRIPTLET = "text/scriptlet";

    /** The Constant TEXT_WEBVIEWHTML. */
    public static final String TEXT_WEBVIEWHTML = "text/webviewhtml";

    /** The Constant TEXT_HTML. */
    public static final String TEXT_HTML = "text/html";

    /** The Constant TEXT_X_SETEXT. */
    public static final String TEXT_X_SETEXT = "text/x-setext";

    /** The Constant TEXT_CSS. */
    public static final String TEXT_CSS = "text/css";

    /** The Constant TEXT_CSV. */
    public static final String TEXT_CSV = "text/csv";

    /** The Constant APPLICATION_JAVASCRIPT. */
    public static final String APPLICATION_JAVASCRIPT = "application/javascript";

    /** The Constant APPLICATION_JSON. */
    public static final String APPLICATION_JSON = "application/json";

    /** The Constant TEXT_XML. */
    public static final String TEXT_XML = "text/xml";


    /** The Constant TEXT_PLAIN. */
    public static final String TEXT_PLAIN = "text/plain";

    /** The Constant APPLICATION_VND_MS_FONTOBJECT. */
    public static final String APPLICATION_VND_MS_FONTOBJECT = "application/vnd.ms-fontobject";

    /** The Constant APPLICATION_FONT_WOFF. */
    public static final String APPLICATION_FONT_WOFF = "application/font-woff";

    /** The Constant APPLICATION_FONT_WOFF2. */
    public static final String APPLICATION_FONT_WOFF2 = "font/woff2";

    /** The Constant APPLICATION_X_FONT_OPENTYPE. */
    public static final String APPLICATION_X_FONT_OPENTYPE = "application/x-font-opentype";

    /** The Constant APPLICATION_X_FONT_TTF. */
    public static final String APPLICATION_X_FONT_TTF = "application/x-font-ttf";

    /** The Constant IMAGE_SVG_XML. */
    public static final String IMAGE_SVG_XML = "image/svg+xml";

    /** The Constant APPLICATION_ZIP. */
    public static final String APPLICATION_ZIP = "application/zip";

    /** The Constant APPLICATION_WASM. */
    public static final String APPLICATION_WASM = "application/wasm";

    /** The Constant APPLICATION_X_COMPRESS. */
    public static final String APPLICATION_X_COMPRESS = "application/x-compress";

    /** The Constant IMAGE_X_XWINDOWDUMP. */
    public static final String IMAGE_X_XWINDOWDUMP = "image/x-xwindowdump";

    /** The Constant IMAGE_X_XPIXMAP. */
    public static final String IMAGE_X_XPIXMAP = "image/x-xpixmap";

    /** The Constant APPLICATION_VND_MS_EXCEL. */
    public static final String APPLICATION_VND_MS_EXCEL = "application/vnd.ms-excel";

    /** The Constant IMAGE_X_XBITMAP. */
    public static final String IMAGE_X_XBITMAP = "image/x-xbitmap";

    /** The Constant APPLICATION_X_MSWRITE. */
    public static final String APPLICATION_X_MSWRITE = "application/x-mswrite";

    /** The Constant APPLICATION_X_MSMETAFILE. */
    public static final String APPLICATION_X_MSMETAFILE = "application/x-msmetafile";

    /** The Constant APPLICATION_VND_MS_WORKS. */
    public static final String APPLICATION_VND_MS_WORKS = "application/vnd.ms-works";

    /** The Constant AUDIO_X_WAV. */
    public static final String AUDIO_X_WAV = "audio/x-wav";

    /** The Constant APPLICATION_X_USTAR. */
    public static final String APPLICATION_X_USTAR = "application/x-ustar";

    /** The Constant APPLICATION_X_MSTERMINAL. */
    public static final String APPLICATION_X_MSTERMINAL = "application/x-msterminal";

    /** The Constant IMAGE_TIFF. */
    public static final String IMAGE_TIFF = "image/tiff";

    /** The Constant APPLICATION_X_COMPRESSED. */
    public static final String APPLICATION_X_COMPRESSED = "application/x-compressed";

    /** The Constant APPLICATION_X_TEXINFO. */
    public static final String APPLICATION_X_TEXINFO = "application/x-texinfo";

    /** The Constant APPLICATION_X_TEX. */
    public static final String APPLICATION_X_TEX = "application/x-tex";

    /** The Constant APPLICATION_X_TCL. */
    public static final String APPLICATION_X_TCL = "application/x-tcl";

    /** The Constant APPLICATION_X_TAR. */
    public static final String APPLICATION_X_TAR = "application/x-tar";

    /** The Constant APPLICATION_X_SHOCKWAVE_FLASH. */
    public static final String APPLICATION_X_SHOCKWAVE_FLASH = "application/x-shockwave-flash";

    /** The Constant APPLICATION_X_SV4CRC. */
    public static final String APPLICATION_X_SV4CRC = "application/x-sv4crc";

    /** The Constant APPLICATION_X_SV4CPIO. */
    public static final String APPLICATION_X_SV4CPIO = "application/x-sv4cpio";

    /** The Constant APPLICATION_VND_MS_PKISTL. */
    public static final String APPLICATION_VND_MS_PKISTL = "application/vnd.ms-pkistl";

    /** The Constant APPLICATION_VND_MS_PKICERTSTORE. */
    public static final String APPLICATION_VND_MS_PKICERTSTORE = "application/vnd.ms-pkicertstore";

    /** The Constant APPLICATION_X_WAIS_SOURCE. */
    public static final String APPLICATION_X_WAIS_SOURCE = "application/x-wais-source";

    /** The Constant APPLICATION_FUTURESPLASH. */
    public static final String APPLICATION_FUTURESPLASH = "application/futuresplash";

    /** The Constant APPLICATION_X_STUFFIT. */
    public static final String APPLICATION_X_STUFFIT = "application/x-stuffit";

    /** The Constant APPLICATION_X_SHAR. */
    public static final String APPLICATION_X_SHAR = "application/x-shar";

    /** The Constant APPLICATION_SET_REGISTRATION_INITIATION. */
    public static final String APPLICATION_SET_REGISTRATION_INITIATION = "application/set-registration-initiation";

    /** The Constant APPLICATION_SET_PAYMENT_INITIATION. */
    public static final String APPLICATION_SET_PAYMENT_INITIATION = "application/set-payment-initiation";

    /** The Constant APPLICATION_X_MSSCHEDULE. */
    public static final String APPLICATION_X_MSSCHEDULE = "application/x-msschedule";

    /** The Constant APPLICATION_X_TROFF. */
    public static final String APPLICATION_X_TROFF = "application/x-troff";

    /** The Constant IMAGE_X_RGB. */
    public static final String IMAGE_X_RGB = "image/x-rgb";

    /** The Constant IMAGE_X_CMU_RASTER. */
    public static final String IMAGE_X_CMU_RASTER = "image/x-cmu-raster";

    /** The Constant AUDIO_X_PN_REALAUDIO. */
    public static final String AUDIO_X_PN_REALAUDIO = "audio/x-pn-realaudio";

    /** The Constant APPLICATION_X_MSPUBLISHER. */
    public static final String APPLICATION_X_MSPUBLISHER = "application/x-mspublisher";

    /** The Constant APPLICATION_PICS_RULES. */
    public static final String APPLICATION_PICS_RULES = "application/pics-rules";

    /** The Constant IMAGE_X_PORTABLE_PIXMAP. */
    public static final String IMAGE_X_PORTABLE_PIXMAP = "image/x-portable-pixmap";

    /** The Constant APPLICATION_VND_MS_POWERPOINT. */
    public static final String APPLICATION_VND_MS_POWERPOINT = "application/vnd.ms-powerpoint";

    /** The Constant IMAGE_X_PORTABLE_ANYMAP. */
    public static final String IMAGE_X_PORTABLE_ANYMAP = "image/x-portable-anymap";

    /** The Constant IMAGE_PNG. */
    public static final String IMAGE_PNG = "image/png";

    /** The Constant APPLICATION_X_PERFMON. */
    public static final String APPLICATION_X_PERFMON = "application/x-perfmon";

    /** The Constant APPLICATION_YND_MS_PKIPKO. */
    public static final String APPLICATION_YND_MS_PKIPKO = "application/ynd.ms-pkipko";

    /** The Constant IMAGE_X_PORTABLE_GRAYMAP. */
    public static final String IMAGE_X_PORTABLE_GRAYMAP = "image/x-portable-graymap";

    /** The Constant APPLICATION_PDF. */
    public static final String APPLICATION_PDF = "application/pdf";

    /** The Constant IMAGE_X_PORTABLE_BITMAP. */
    public static final String IMAGE_X_PORTABLE_BITMAP = "image/x-portable-bitmap";

    /** The Constant APPLICATION_X_PKCS7_SIGNATURE. */
    public static final String APPLICATION_X_PKCS7_SIGNATURE = "application/x-pkcs7-signature";

    /** The Constant APPLICATION_X_PKCS7_CERTREQRESP. */
    public static final String APPLICATION_X_PKCS7_CERTREQRESP = "application/x-pkcs7-certreqresp";

    /** The Constant APPLICATION_X_PKCS7_MIME. */
    public static final String APPLICATION_X_PKCS7_MIME = "application/x-pkcs7-mime";

    /** The Constant APPLICATION_X_PKCS7_CERTIFICATES. */
    public static final String APPLICATION_X_PKCS7_CERTIFICATES = "application/x-pkcs7-certificates";

    /** The Constant APPLICATION_X_PKCS12. */
    public static final String APPLICATION_X_PKCS12 = "application/x-pkcs12";

    /** The Constant APPLICATION_PKCS10. */
    public static final String APPLICATION_PKCS10 = "application/pkcs10";

    /** The Constant APPLICATION_ODA. */
    public static final String APPLICATION_ODA = "application/oda";

    /** The Constant APPLICATION_VND_MS_OUTLOOK. */
    public static final String APPLICATION_VND_MS_OUTLOOK = "application/vnd.ms-outlook";

    /** The Constant APPLICATION_X_TROFF_MS. */
    public static final String APPLICATION_X_TROFF_MS = "application/x-troff-ms";

    /** The Constant APPLICATION_VND_MS_PROJECT. */
    public static final String APPLICATION_VND_MS_PROJECT = "application/vnd.ms-project";

    /** The Constant AUDIO_MPEG. */
    public static final String AUDIO_MPEG = "audio/mpeg";

    /** The Constant VIDEO_MPEG. */
    public static final String VIDEO_MPEG = "video/mpeg";

    /** The Constant VIDEO_X_SGI_MOVIE. */
    public static final String VIDEO_X_SGI_MOVIE = "video/x-sgi-movie";

    /** The Constant VIDEO_QUICKTIME. */
    public static final String VIDEO_QUICKTIME = "video/quicktime";

    /** The Constant APPLICATION_X_MSMONEY. */
    public static final String APPLICATION_X_MSMONEY = "application/x-msmoney";

    /** The Constant AUDIO_MID. */
    public static final String AUDIO_MID = "audio/mid";

    /** The Constant MESSAGE_RFC822. */
    public static final String MESSAGE_RFC822 = "message/rfc822";

    /** The Constant APPLICATION_X_TROFF_ME. */
    public static final String APPLICATION_X_TROFF_ME = "application/x-troff-me";

    /** The Constant APPLICATION_X_MSACCESS. */
    public static final String APPLICATION_X_MSACCESS = "application/x-msaccess";

    /** The Constant APPLICATION_X_TROFF_MAN. */
    public static final String APPLICATION_X_TROFF_MAN = "application/x-troff-man";

    /** The Constant AUDIO_X_MPEGURL. */
    public static final String AUDIO_X_MPEGURL = "audio/x-mpegurl";

    /** The Constant APPLICATION_X_MSMEDIAVIEW. */
    public static final String APPLICATION_X_MSMEDIAVIEW = "application/x-msmediaview";

    /** The Constant VIDEO_X_LA_ASF. */
    public static final String VIDEO_X_LA_ASF = "video/x-la-asf";

    /** The Constant APPLICATION_X_LATEX. */
    public static final String APPLICATION_X_LATEX = "application/x-latex";

    /** The Constant IMAGE_JPEG. */
    public static final String IMAGE_JPEG = "image/jpeg";

    /** The Constant IMAGE_PIPEG. */
    public static final String IMAGE_PIPEG = "image/pipeg";

    /** The Constant APPLICATION_X_INTERNET_SIGNUP. */
    public static final String APPLICATION_X_INTERNET_SIGNUP = "application/x-internet-signup";

    /** The Constant APPLICATION_X_IPHONE. */
    public static final String APPLICATION_X_IPHONE = "application/x-iphone";

    /** The Constant IMAGE_IEF. */
    public static final String IMAGE_IEF = "image/ief";

    /** The Constant IMAGE_X_ICON. */
    public static final String IMAGE_X_ICON = "image/x-icon";

    /** The Constant IMAGE_WEBP. */
    public static final String IMAGE_WEBP = "image/webp";

    /** The Constant APPLICATION_HTA. */
    public static final String APPLICATION_HTA = "application/hta";

    /** The Constant APPLICATION_MAC_BINHEX40. */
    public static final String APPLICATION_MAC_BINHEX40 = "application/mac-binhex40";

    /** The Constant APPLICATION_WINHLP. */
    public static final String APPLICATION_WINHLP = "application/winhlp";

    /** The Constant APPLICATION_X_HDF. */
    public static final String APPLICATION_X_HDF = "application/x-hdf";

    /** The Constant APPLICATION_X_GZIP. */
    public static final String APPLICATION_X_GZIP = "application/x-gzip";

    /** The Constant APPLICATION_X_GTAR. */
    public static final String APPLICATION_X_GTAR = "application/x-gtar";

    /** The Constant IMAGE_GIF. */
    public static final String IMAGE_GIF = "image/gif";

    /** The Constant X_WORLD_X_VRML. */
    public static final String X_WORLD_X_VRML = "x-world/x-vrml";

    /** The Constant APPLICATION_FRACTALS. */
    public static final String APPLICATION_FRACTALS = "application/fractals";

    /** The Constant APPLICATION_ENVOY. */
    public static final String APPLICATION_ENVOY = "application/envoy";

    /** The Constant APPLICATION_X_DVI. */
    public static final String APPLICATION_X_DVI = "application/x-dvi";

    /** The Constant APPLICATION_MSWORD. */
    public static final String APPLICATION_MSWORD = "application/msword";

    /** The Constant APPLICATION_X_MSDOWNLOAD. */
    public static final String APPLICATION_X_MSDOWNLOAD = "application/x-msdownload";

    /** The Constant APPLICATION_X_DIRECTOR. */
    public static final String APPLICATION_X_DIRECTOR = "application/x-director";

    /** The Constant APPLICATION_X_CSH. */
    public static final String APPLICATION_X_CSH = "application/x-csh";

    /** The Constant APPLICATION_PKIX_CRL. */
    public static final String APPLICATION_PKIX_CRL = "application/pkix-crl";

    /** The Constant APPLICATION_X_MSCARDFILE. */
    public static final String APPLICATION_X_MSCARDFILE = "application/x-mscardfile";

    /** The Constant APPLICATION_X_CPIO. */
    public static final String APPLICATION_X_CPIO = "application/x-cpio";

    /** The Constant IMAGE_CIS_COD. */
    public static final String IMAGE_CIS_COD = "image/cis-cod";

    /** The Constant IMAGE_X_CMX. */
    public static final String IMAGE_X_CMX = "image/x-cmx";

    /** The Constant APPLICATION_X_MSCLIP. */
    public static final String APPLICATION_X_MSCLIP = "application/x-msclip";

    /** The Constant APPLICATION_X_X509_CA_CERT. */
    public static final String APPLICATION_X_X509_CA_CERT = "application/x-x509-ca-cert";

    /** The Constant APPLICATION_X_NETCDF. */
    public static final String APPLICATION_X_NETCDF = "application/x-netcdf";

    /** The Constant APPLICATION_X_CDF. */
    public static final String APPLICATION_X_CDF = "application/x-cdf";

    /** The Constant APPLICATION_VND_MS_PKISECCAT. */
    public static final String APPLICATION_VND_MS_PKISECCAT = "application/vnd.ms-pkiseccat";

    /** The Constant IMAGE_BMP. */
    public static final String IMAGE_BMP = "image/bmp";

    /** The Constant APPLICATION_OCTET_STREAM. */
    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    /** The Constant APPLICATION_X_BCPIO. */
    public static final String APPLICATION_X_BCPIO = "application/x-bcpio";

    /** The Constant APPLICATION_OLESCRIPT. */
    public static final String APPLICATION_OLESCRIPT = "application/olescript";

    /** The Constant VIDEO_X_MSVIDEO. */
    public static final String VIDEO_X_MSVIDEO = "video/x-msvideo";

    /** The Constant AUDIO_BASIC. */
    public static final String AUDIO_BASIC = "audio/basic";

    /** The Constant VIDEO_X_MS_ASF. */
    public static final String VIDEO_X_MS_ASF = "video/x-ms-asf";

    /** The Constant APPLICATION_VND_ANDROID_PACKAGE_ARCHIVE. */
    public static final String APPLICATION_VND_ANDROID_PACKAGE_ARCHIVE = "application/vnd.android.package-archive";

    /** The Constant AUDIO_X_AIFF. */
    public static final String AUDIO_X_AIFF = "audio/x-aiff";

    /** The Constant APPLICATION_HDBTI. */
    public static final String APPLICATION_HDBTI = "application/hdbti";

    /** The Constant APPLICATION_POSTSCRIPT. */
    public static final String APPLICATION_POSTSCRIPT = "application/postscript";

    /** The Constant APPLICATION_BPMN. */
    public static final String APPLICATION_BPMN = "application/bpmn+xml";

    /** The Constant APPLICATION_CAMEL. */
    public static final String APPLICATION_CAMEL = "application/camel";

    /** The Constant APPLICATION_SCHEMA. */
    public static final String APPLICATION_SCHEMA = "application/database-schema-model+xml";

    /** The Constant APPLICATION_ENTITY_DATA_MODEL. */
    public static final String APPLICATION_ENTITY_DATA_MODEL = "application/entity-data-model+xml";

    /** The Constant APPLICATION_DATA_MAPPING. */
    public static final String APPLICATION_DATA_MAPPING = "application/data-mapping-model+xml";

    /** The Constant APPLICATION_JSON_JOB. */
    public static final String APPLICATION_JSON_JOB = "application/json+job";

    /** The Constant APPLICATION_JSON_XSJOB. */
    public static final String APPLICATION_JSON_XSJOB = "application/json+xsjob";

    /** The Constant APPLICATION_XML_CALCULATIONVIEW. */
    public static final String APPLICATION_XML_CALCULATIONVIEW = "application/xml+calculationview";

    /** The Constant APPLICATION_JSON_LISTENER. */
    public static final String APPLICATION_JSON_LISTENER = "application/json+listener";

    /** The Constant APPLICATION_JSON_WEBSOCKET. */
    public static final String APPLICATION_JSON_WEBSOCKET = "application/json+websocket";

    /** The Constant APPLICATION_JSON_TABLE. */
    public static final String APPLICATION_JSON_TABLE = "application/json+table";

    /** The Constant APPLICATION_JSON_VIEW. */
    public static final String APPLICATION_JSON_VIEW = "application/json+view";

    /** The Constant APPLICATION_JSON_ACCESS. */
    public static final String APPLICATION_JSON_ACCESS = "application/json+access";

    /** The Constant APPLICATION_JSON_ROLES. */
    public static final String APPLICATION_JSON_ROLES = "application/json+roles";

    /** The Constant APPLICATION_JSON_CSVIM. */
    public static final String APPLICATION_JSON_CSVIM = "application/json+csvim";

    /** The Constant APPLICATION_JSON_EXTENSION_POINT. */
    public static final String APPLICATION_JSON_EXTENSION_POINT = "application/json+extension-point";

    /** The Constant APPLICATION_JSON_EXTENSION. */
    public static final String APPLICATION_JSON_EXTENSION = "application/json+extension";

    /** The Constant APPLICATION_JSON_COMMAND. */
    public static final String APPLICATION_JSON_COMMAND = "application/json+command";

    /** The Constant APPLICATION_JSON_FORM. */
    public static final String APPLICATION_JSON_FORM = "application/json+form";

    /** The Constant APPLICATION_JSON_XSACCESS. */
    public static final String APPLICATION_JSON_XSACCESS = "application/json+xsaccess";

    /** The Constant APPLICATION_JSON_VIEW. */
    public static final String APPLICATION_JSON_REPORT = "application/json+report";

    /** The Constant APPLICATION_JSON_HDI. */
    public static final String APPLICATION_JSON_HDI = "application/json+hdi";

    /** The Constant CONTENT_TYPES. */
    private static final Map<String, String> CONTENT_TYPES = new HashMap<String, String>();

    /** The Constant BINARY_CONTENT_TYPES. */
    private static final Map<String, String> BINARY_CONTENT_TYPES = new HashMap<String, String>();

    /** The Constant TEXT_CONTENT_TYPES. */
    private static final Map<String, String> TEXT_CONTENT_TYPES = new HashMap<String, String>();

    static {
        BINARY_CONTENT_TYPES.put("ai", APPLICATION_POSTSCRIPT); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("aif", AUDIO_X_AIFF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("aifc", AUDIO_X_AIFF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("aiff", AUDIO_X_AIFF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("apk", APPLICATION_VND_ANDROID_PACKAGE_ARCHIVE); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("asf", VIDEO_X_MS_ASF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("asr", VIDEO_X_MS_ASF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("asx", VIDEO_X_MS_ASF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("au", AUDIO_BASIC); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("avi", VIDEO_X_MSVIDEO); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("axs", APPLICATION_OLESCRIPT); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("bcpio", APPLICATION_X_BCPIO); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("bin", APPLICATION_OCTET_STREAM); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("bmp", IMAGE_BMP); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("cat", APPLICATION_VND_MS_PKISECCAT); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("cdf", APPLICATION_X_CDF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("cdf", APPLICATION_X_NETCDF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("cer", APPLICATION_X_X509_CA_CERT); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("class", APPLICATION_OCTET_STREAM); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("clp", APPLICATION_X_MSCLIP); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("cmx", IMAGE_X_CMX); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("cod", IMAGE_CIS_COD); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("cpio", APPLICATION_X_CPIO); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("crd", APPLICATION_X_MSCARDFILE); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("crl", APPLICATION_PKIX_CRL); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("crt", APPLICATION_X_X509_CA_CERT); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("csh", APPLICATION_X_CSH); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("dcr", APPLICATION_X_DIRECTOR); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("der", APPLICATION_X_X509_CA_CERT); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("dir", APPLICATION_X_DIRECTOR); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("dll", APPLICATION_X_MSDOWNLOAD); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("dms", APPLICATION_OCTET_STREAM); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("doc", APPLICATION_MSWORD); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("dot", APPLICATION_MSWORD); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("dvi", APPLICATION_X_DVI); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("dxr", APPLICATION_X_DIRECTOR); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("eps", APPLICATION_POSTSCRIPT); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("evy", APPLICATION_ENVOY); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("exe", APPLICATION_OCTET_STREAM); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("fif", APPLICATION_FRACTALS); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("flr", X_WORLD_X_VRML); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("gif", IMAGE_GIF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("gtar", APPLICATION_X_GTAR); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("gz", APPLICATION_X_GZIP); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("hdf", APPLICATION_X_HDF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("hlp", APPLICATION_WINHLP); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("hqx", APPLICATION_MAC_BINHEX40); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("hta", APPLICATION_HTA); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("ico", IMAGE_X_ICON); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("ief", IMAGE_IEF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("iii", APPLICATION_X_IPHONE); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("ins", APPLICATION_X_INTERNET_SIGNUP); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("isp", APPLICATION_X_INTERNET_SIGNUP); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("jfif", IMAGE_PIPEG); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("jpe", IMAGE_JPEG); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("jpeg", IMAGE_JPEG); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("jpg", IMAGE_JPEG); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("webp", IMAGE_WEBP); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("latex", APPLICATION_X_LATEX); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("lha", APPLICATION_OCTET_STREAM); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("lsf", VIDEO_X_LA_ASF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("lsx", VIDEO_X_LA_ASF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("lzh", APPLICATION_OCTET_STREAM); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("m13", APPLICATION_X_MSMEDIAVIEW); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("m14", APPLICATION_X_MSMEDIAVIEW); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("m3u", AUDIO_X_MPEGURL); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("man", APPLICATION_X_TROFF_MAN); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("mdb", APPLICATION_X_MSACCESS); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("me", APPLICATION_X_TROFF_ME); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("mht", MESSAGE_RFC822); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("mhtml", MESSAGE_RFC822); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("mid", AUDIO_MID); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("mny", APPLICATION_X_MSMONEY); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("mov", VIDEO_QUICKTIME); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("movie", VIDEO_X_SGI_MOVIE); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("mp2", VIDEO_MPEG); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("mp3", AUDIO_MPEG); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("mpa", VIDEO_MPEG); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("mpe", VIDEO_MPEG); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("mpeg", VIDEO_MPEG); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("mpg", VIDEO_MPEG); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("mpp", APPLICATION_VND_MS_PROJECT); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("mpv2", VIDEO_MPEG); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("ms", APPLICATION_X_TROFF_MS); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("msg", APPLICATION_VND_MS_OUTLOOK); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("mvb", APPLICATION_X_MSMEDIAVIEW); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("nc", APPLICATION_X_NETCDF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("nws", MESSAGE_RFC822); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("oda", APPLICATION_ODA); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("p10", APPLICATION_PKCS10); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("p12", APPLICATION_X_PKCS12); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("p7b", APPLICATION_X_PKCS7_CERTIFICATES); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("p7c", APPLICATION_X_PKCS7_MIME); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("p7m", APPLICATION_X_PKCS7_MIME); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("p7r", APPLICATION_X_PKCS7_CERTREQRESP); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("p7s", APPLICATION_X_PKCS7_SIGNATURE); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("pbm", IMAGE_X_PORTABLE_BITMAP); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("pdf", APPLICATION_PDF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("pfx", APPLICATION_X_PKCS12); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("pgm", IMAGE_X_PORTABLE_GRAYMAP); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("pko", APPLICATION_YND_MS_PKIPKO); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("pma", APPLICATION_X_PERFMON); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("pmc", APPLICATION_X_PERFMON); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("pml", APPLICATION_X_PERFMON); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("pmr", APPLICATION_X_PERFMON); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("pmw", APPLICATION_X_PERFMON); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("png", IMAGE_PNG); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("pnm", IMAGE_X_PORTABLE_ANYMAP); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("pot", APPLICATION_VND_MS_POWERPOINT); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("ppm", IMAGE_X_PORTABLE_PIXMAP); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("pps", APPLICATION_VND_MS_POWERPOINT); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("ppt", APPLICATION_VND_MS_POWERPOINT); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("prf", APPLICATION_PICS_RULES); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("ps", APPLICATION_POSTSCRIPT); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("pub", APPLICATION_X_MSPUBLISHER); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("qt", VIDEO_QUICKTIME); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("ra", AUDIO_X_PN_REALAUDIO); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("ram", AUDIO_X_PN_REALAUDIO); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("ras", IMAGE_X_CMU_RASTER); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("rgb", IMAGE_X_RGB); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("rmi", AUDIO_MID); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("roff", APPLICATION_X_TROFF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("scd", APPLICATION_X_MSSCHEDULE); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("setpay", APPLICATION_SET_PAYMENT_INITIATION); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("setreg", APPLICATION_SET_REGISTRATION_INITIATION); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("shar", APPLICATION_X_SHAR); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("sit", APPLICATION_X_STUFFIT); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("snd", AUDIO_BASIC); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("spc", APPLICATION_X_PKCS7_CERTIFICATES); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("spl", APPLICATION_FUTURESPLASH); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("src", APPLICATION_X_WAIS_SOURCE); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("sst", APPLICATION_VND_MS_PKICERTSTORE); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("stl", APPLICATION_VND_MS_PKISTL); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("sv4cpio", APPLICATION_X_SV4CPIO); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("sv4crc", APPLICATION_X_SV4CRC); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("swf", APPLICATION_X_SHOCKWAVE_FLASH); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("t", APPLICATION_X_TROFF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("tar", APPLICATION_X_TAR); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("tcl", APPLICATION_X_TCL); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("tex", APPLICATION_X_TEX); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("texi", APPLICATION_X_TEXINFO); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("texinfo", APPLICATION_X_TEXINFO); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("tgz", APPLICATION_X_COMPRESSED); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("tif", IMAGE_TIFF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("tiff", IMAGE_TIFF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("tr", APPLICATION_X_TROFF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("trm", APPLICATION_X_MSTERMINAL); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("ustar", APPLICATION_X_USTAR); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("vrml", X_WORLD_X_VRML); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("wav", AUDIO_X_WAV); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("wcm", APPLICATION_VND_MS_WORKS); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("wdb", APPLICATION_VND_MS_WORKS); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("wks", APPLICATION_VND_MS_WORKS); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("wmf", APPLICATION_X_MSMETAFILE); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("wps", APPLICATION_VND_MS_WORKS); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("wri", APPLICATION_X_MSWRITE); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("wrl", X_WORLD_X_VRML); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("wrz", X_WORLD_X_VRML); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("xaf", X_WORLD_X_VRML); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("xbm", IMAGE_X_XBITMAP); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("xla", APPLICATION_VND_MS_EXCEL); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("xlc", APPLICATION_VND_MS_EXCEL); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("xlm", APPLICATION_VND_MS_EXCEL); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("xls", APPLICATION_VND_MS_EXCEL); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("xlt", APPLICATION_VND_MS_EXCEL); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("xlw", APPLICATION_VND_MS_EXCEL); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("xof", X_WORLD_X_VRML); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("xpm", IMAGE_X_XPIXMAP); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("xwd", IMAGE_X_XWINDOWDUMP); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("z", APPLICATION_X_COMPRESS); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("zip", APPLICATION_ZIP); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("wasm", APPLICATION_WASM); //$NON-NLS-1$

        BINARY_CONTENT_TYPES.put("ttf", APPLICATION_OCTET_STREAM); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("svg", IMAGE_SVG_XML); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("ttf", APPLICATION_X_FONT_TTF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("otf", APPLICATION_X_FONT_OPENTYPE); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("woff", APPLICATION_FONT_WOFF); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("woff2", APPLICATION_FONT_WOFF2); //$NON-NLS-1$
        BINARY_CONTENT_TYPES.put("eot", APPLICATION_VND_MS_FONTOBJECT); //$NON-NLS-1$

        TEXT_CONTENT_TYPES.put("txt", TEXT_PLAIN); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("xml", TEXT_XML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("json", APPLICATION_JSON); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("js", APPLICATION_JAVASCRIPT); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("mjs", APPLICATION_JAVASCRIPT); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("xsjs", APPLICATION_JAVASCRIPT); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("xsjslib", APPLICATION_JAVASCRIPT); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("calculationview", APPLICATION_XML_CALCULATIONVIEW); //$NON-NLS-1$
        // TEXT_CONTENT_TYPES.put("jslib", APPLICATION_JAVASCRIPT); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("table", APPLICATION_JSON_TABLE); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("view", APPLICATION_JSON_VIEW); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("job", APPLICATION_JSON_JOB); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("xsjob", APPLICATION_JSON_XSJOB); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("listener", APPLICATION_JSON_LISTENER); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("websocket", APPLICATION_JSON_WEBSOCKET); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("extensionpoint", APPLICATION_JSON_EXTENSION_POINT); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("extension", APPLICATION_JSON_EXTENSION); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("access", APPLICATION_JSON_ACCESS); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("roles", APPLICATION_JSON_ROLES); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("hdbti", APPLICATION_HDBTI); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("csvim", APPLICATION_JSON_CSVIM); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("command", APPLICATION_JSON_COMMAND); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("form", APPLICATION_JSON_FORM); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("xsaccess", APPLICATION_JSON_XSACCESS); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("report", APPLICATION_JSON_REPORT); //$NON-NLS-1$
        // TEXT_CONTENT_TYPES.put("entity", APPLICATION_JSON); //$NON-NLS-1$
        // TEXT_CONTENT_TYPES.put("ws", APPLICATION_JSON); //$NON-NLS-1$
        // TEXT_CONTENT_TYPES.put("routes", TEXT_XML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("wsdl", TEXT_XML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("xsl", TEXT_XML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("xslt", TEXT_XML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("bas", TEXT_PLAIN); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("c", TEXT_PLAIN); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("css", TEXT_CSS); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("csv", TEXT_CSV); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("etx", TEXT_X_SETEXT); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("h", TEXT_PLAIN); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("htm", TEXT_HTML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("html", TEXT_HTML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("htt", TEXT_WEBVIEWHTML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("sct", TEXT_SCRIPTLET); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("rtx", TEXT_RICHTEXT); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("rtf", APPLICATION_RTF); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("sh", APPLICATION_X_SH); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("stm", TEXT_HTML); //$NON-NLS-1$
        // TEXT_CONTENT_TYPES.put("svg", "image/svg+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        TEXT_CONTENT_TYPES.put("tsv", TEXT_TAB_SEPARATED_VALUES); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("dsv", TEXT_DELIMITER_SEPARATED_VALUES); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("append", TEXT_DELIMITER_SEPARATED_VALUES); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("replace", TEXT_DELIMITER_SEPARATED_VALUES); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("delete", TEXT_DELIMITER_SEPARATED_VALUES); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("update", TEXT_DELIMITER_SEPARATED_VALUES); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("uls", TEXT_IULS); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("htc", TEXT_X_COMPONENT); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("vcf", TEXT_X_VCARD); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("wiki", TEXT_HTML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("wikis", TEXT_HTML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("md", TEXT_HTML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("markdown", TEXT_HTML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("mdown", TEXT_HTML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("mkdn", TEXT_HTML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("markdown", TEXT_HTML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("mkd", TEXT_HTML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("mdwn", TEXT_HTML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("textile", TEXT_HTML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("confluence", TEXT_HTML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("tracwiki", TEXT_HTML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("twiki", TEXT_HTML); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("bpmn", APPLICATION_BPMN); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("camel", APPLICATION_CAMEL); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("dsm", APPLICATION_SCHEMA); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("edm", APPLICATION_ENTITY_DATA_MODEL); //$NON-NLS-1$
        TEXT_CONTENT_TYPES.put("dmm", APPLICATION_DATA_MAPPING); // $NON-NLS-1
        TEXT_CONTENT_TYPES.put("hdi", APPLICATION_JSON_HDI); //$NON-NLS-1$


        CONTENT_TYPES.putAll(BINARY_CONTENT_TYPES);
        CONTENT_TYPES.putAll(TEXT_CONTENT_TYPES);

    }

    /** The Constant DEFAULT_CONTENT_TYPE. */
    public static final String DEFAULT_CONTENT_TYPE = TEXT_PLAIN;

    /**
     * Gets the content type.
     *
     * @param extension the extension
     * @return the content type
     */
    public static String getContentType(String extension) {
        if (extension == null) {
            return DEFAULT_CONTENT_TYPE;
        }
        String contentType = CONTENT_TYPES.get(extension.toLowerCase());
        if (contentType == null) {
            contentType = DEFAULT_CONTENT_TYPE;
        }

        return contentType;
    }

    /**
     * Checks if is binary.
     *
     * @param contentType the content type
     * @return true, if is binary
     */
    public static boolean isBinary(String contentType) {
        return BINARY_CONTENT_TYPES.containsValue(contentType);
    }

    /**
     * Gets the extension.
     *
     * @param filename the filename
     * @return the extension
     */
    public static String getExtension(String filename) {
        return FilenameUtils.getExtension(filename);
    }
}
