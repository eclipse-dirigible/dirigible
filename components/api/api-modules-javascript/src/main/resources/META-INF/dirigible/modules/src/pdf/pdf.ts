import { Registry } from "../platform/registry";
import { XML } from "../utils/xml";
import { TemplateEngines } from "../template";
const PDFFacade = Java.type("org.eclipse.dirigible.components.api.pdf.PDFFacade");

// Path to the default table template
const TEMPLATE_PATH_TABLE = "pdf/templates/table.xml";

/**
 * Interface defining the structured data required to generate a table in a PDF.
 */
export interface PDFTableData {
    title: string;
    description: string;
    columns: {
        name: string // Display name of the column
        key: string  // Data key to look up in the rows
    }[],
    rows: { [key: string]: any }[] // Array of data objects for each row
}

/**
 * Interface defining optional configuration for PDF document layout.
 */
export interface PDFTableConfig {
    /** Document width in mm. Overrides size if present. */
    pageWidth?: number;
    /** Document height in mm. Overrides size if present. */
    pageHeight?: number;
    /** Whether to apply column alignment (based on template default). */
    alignColumns?: boolean;
    /** Whether to apply row alignment (based on template default). */
    alignRows?: boolean;
    /** Standard ISO 216 paper size (A0-A10). Sets standard dimensions if custom width/height are not provided. */
    size?: "a0" | "a1" | "a2" | "a3" | "a4" | "a5" | "a6" | "a7" | "a8" | "a9" | "a10";
}

/**
 * Internal interface for template parameters, including data and configuration.
 */
interface TemplateParameters extends PDFTableData {
    pageWidth: string;
    pageHeight: string;
    // Note: alignColumns/Rows are typed as string here to match the default value ("center")
    // but may be overwritten by booleans from config due to the original logic.
    alignColumns: string | boolean;
    alignRows: string | boolean;
}

/**
 * @class PDF
 * @description Utility class for generating PDF documents using a template engine and the PDFFacade.
 */
export class PDF {

    /**
     * Generates a PDF document containing a styled table based on the standard table template.
     *
     * @param {PDFTableData} data The structured data to populate the table.
     * @param {PDFTableConfig} [config] Optional configuration for page size and alignment.
     * @returns {any[]} The generated PDF content as a byte array (Array<number>).
     */
    public static generateTable(data: PDFTableData, config?: PDFTableConfig): any[] {
        const defaultTemplateParameters: Omit<TemplateParameters, keyof PDFTableData> = {
            // Default A4 size in mm
            pageWidth: "210",
            pageHeight: "297",
            alignColumns: "center",
            alignRows: "center"
        };

        let templateParameters: TemplateParameters = {
            ...defaultTemplateParameters,
            ...data
        } as TemplateParameters; // Cast needed due to the merging of types

        PDF.setTemplateParameters(templateParameters, config);

        const template = Registry.getText(TEMPLATE_PATH_TABLE);
        const pdfTemplate = TemplateEngines.generate(TEMPLATE_PATH_TABLE, template, templateParameters);

        // Convert data payload to XML format expected by the underlying Java PDFFacade
        const xmlData = XML.fromJson({
            content: data
        });
        return PDFFacade.generate(pdfTemplate, xmlData);
    }

    /**
     * Generates a PDF document using a custom template path and data payload.
     *
     * @param {string} templatePath The path to the custom template file (e.g., in the Registry).
     * @param {PDFTableData} data The data to be injected into the template.
     * @returns {any[]} The generated PDF content as a byte array (Array<number>).
     */
    public static generate(templatePath: string, data: PDFTableData): any[] {
        const template = Registry.getText(templatePath);

        // Convert data payload to XML format expected by the underlying Java PDFFacade
        const xmlData = XML.fromJson({
            content: data
        });
        return PDFFacade.generate(template, xmlData);
    }

    /**
     * Internal method to set template parameters based on optional configuration.
     *
     * @param {TemplateParameters} templateParameters The object containing parameters to be modified.
     * @param {PDFTableConfig} [config] The optional configuration object.
     */
    private static setTemplateParameters(templateParameters: TemplateParameters, config?: PDFTableConfig): void {
        PDF.setDocumentSize(templateParameters, config);
        PDF.setDocumentAlign(templateParameters, config);
    }

    /**
     * Internal method to set column and row alignment parameters.
     *
     * @param {TemplateParameters} templateParameters The object containing parameters to be modified.
     * @param {PDFTableConfig} [config] The optional configuration object.
     */
    private static setDocumentAlign(templateParameters: TemplateParameters, config?: PDFTableConfig): void {
        // Only override if the config value is explicitly present and truthy (maintaining original behavior)
        if (config?.alignColumns) {
            templateParameters.alignColumns = config.alignColumns;
        }
        if (config?.alignRows) {
            templateParameters.alignRows = config.alignRows;
        }
    }

    /**
     * Internal method to set the document size (width and height in mm) based on a standard 'size' or custom dimensions.
     *
     * @param {TemplateParameters} templateParameters The object containing parameters to be modified.
     * @param {PDFTableConfig} [config] The optional configuration object.
     */
    private static setDocumentSize(templateParameters: TemplateParameters, config?: PDFTableConfig): void {
        if (config?.pageWidth !== undefined) {
            templateParameters.pageWidth = String(config.pageWidth);
        }
        if (config?.pageHeight !== undefined) {
            templateParameters.pageHeight = String(config.pageHeight);
        }

        if (config?.size) {
            // Mapping ISO 216 paper sizes (in millimeters)
            switch (config.size.toLowerCase()) {
                case "a0":
                    templateParameters.pageWidth = "841";
                    templateParameters.pageHeight = "1189";
                    break;
                case "a1":
                    templateParameters.pageWidth = "594";
                    templateParameters.pageHeight = "841";
                    break;
                case "a2":
                    templateParameters.pageWidth = "420";
                    templateParameters.pageHeight = "594";
                    break;
                case "a3":
                    templateParameters.pageWidth = "297";
                    templateParameters.pageHeight = "420";
                    break;
                case "a4": // Default
                    templateParameters.pageWidth = "210";
                    templateParameters.pageHeight = "297";
                    break;
                case "a5":
                    templateParameters.pageWidth = "148";
                    templateParameters.pageHeight = "210";
                    break;
                case "a6":
                    templateParameters.pageWidth = "105";
                    templateParameters.pageHeight = "148";
                    break;
                case "a7":
                    templateParameters.pageWidth = "74";
                    templateParameters.pageHeight = "105";
                    break;
                case "a8":
                    templateParameters.pageWidth = "52";
                    templateParameters.pageHeight = "74";
                    break;
                case "a9":
                    templateParameters.pageWidth = "37";
                    templateParameters.pageHeight = "52";
                    break;
                case "a10":
                    templateParameters.pageWidth = "26";
                    templateParameters.pageHeight = "37";
                    break;
            }
        }
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = PDF;
}
