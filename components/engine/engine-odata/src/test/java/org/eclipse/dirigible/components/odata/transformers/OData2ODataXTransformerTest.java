/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.odata.transformers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.sql.SQLException;
import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.domain.TableColumn;
import org.eclipse.dirigible.components.odata.api.ODataParameter;
import org.eclipse.dirigible.components.odata.api.ODataProperty;
import org.eclipse.dirigible.components.odata.domain.OData;
import org.eclipse.dirigible.components.odata.factory.ODataDefinitionFactoryTest;
import org.eclipse.dirigible.components.odata.synchronizer.ODataSynchronizer;
import org.eclipse.dirigible.database.sql.ISqlKeywords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

/**
 * The Class OData2ODataXTransformerTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@ExtendWith(MockitoExtension.class)
public class OData2ODataXTransformerTest {

    /** The db metadata util. */
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ODataDatabaseMetadataUtil odataDatabaseMetadataUtil;

    /** The default table metadata provider. */
    private DefaultTableMetadataProvider defaultTableMetadataProvider;

    /**
     * Sets the up.
     */
    @BeforeEach
    void setUp() {
        this.defaultTableMetadataProvider = new DefaultTableMetadataProvider(odataDatabaseMetadataUtil);
    }

    /**
     * Test transform with incorrect multiplicity.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testTransformWithIncorrectMultiplicity() throws IOException, SQLException {
        try {
            byte[] employee = IOUtils.toByteArray(
                    ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeWithWrongMultiplicity.odata"));
            OData definition = ODataSynchronizer.parseOData("/transformers/EmployeeWithWrongMultiplicity.odata", employee);

            Table model = new Table("EMPLOYEES");
            new TableColumn("COMPANY_ID", "Edm.Int32", "0", true, true, model);
            new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", "0", true, true, model);

            model.setKind(ISqlKeywords.METADATA_TABLE);
            when(odataDatabaseMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);

            new OData2ODataXTransformer(defaultTableMetadataProvider).transform(definition);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    /**
     * Test transform with composite primary key with less db props exposed.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testTransformWithCompositePrimaryKeyWithLessDbPropsExposed() throws IOException, SQLException {
        byte[] employee = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeWithProp.odata"));
        OData definition = ODataSynchronizer.parseOData("/transformers/EmployeeWithProp.odata", employee);

        Table model = new Table("EMPLOYEES");
        new TableColumn("COMPANY_ID", "Edm.Int32", "0", false, true, model);
        new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", "0", false, true, model);
        new TableColumn("ORDER_ID", "Edm.Int32", "0", model);
        new TableColumn("ADDRESS_ID", "Edm.Int32", "0", model);

        model.setKind(ISqlKeywords.METADATA_TABLE);
        when(odataDatabaseMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);

        model = new Table("PHONES");
        new TableColumn("NUMBER", "Edm.Int32", "0", true, true, model);
        new TableColumn("FK_COMPANY_ID", "Edm.Int32", "0", model);
        new TableColumn("FK_EMPLOYEE_NUMBER", "Edm.Int32", "0", model);

        model.setKind(ISqlKeywords.METADATA_TABLE);
        when(odataDatabaseMetadataUtil.getTableMetadata("PHONES", null)).thenReturn(model);

        String entitySchema = "<Schema Namespace=\"np\"\n" + "\txmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\">\n"
                + "\t<EntityType Name=\"employeeType\">\n" + "\t\t<Key>\n" + "\t\t\t<PropertyRef Name=\"companyId\" />\n"
                + "\t\t\t<PropertyRef Name=\"employeeNumber\" />\n" + "\t\t</Key>\n"
                + "\t\t<Property Name=\"companyId\" Nullable=\"false\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"employeeNumber\" Nullable=\"false\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"orderId\" Nullable=\"false\" Type=\"Edm.Int32\"/>\n" + "\t</EntityType>\n"
                + "\t<EntityType Name=\"phoneType\">\n" + "\t\t<Key>\n" + "\t\t\t<PropertyRef Name=\"Number\" />\n" + "\t\t</Key>\n"
                + "\t\t<Property Name=\"Number\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"FkCompanyId\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"FkEmployeeNumber\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n" + "\t</EntityType>\n" + "</Schema>\n";
        String entitySet = "\t\t<EntitySet Name=\"Employees\" EntityType=\"np.employeeType\"/>\n"
                + "\t\t<EntitySet Name=\"Phones\" EntityType=\"np.phoneType\"/>\n";

        String[] actualResult = new OData2ODataXTransformer(defaultTableMetadataProvider).transform(definition);
        assertArrayEquals(new String[] {entitySchema, entitySet}, actualResult);
    }

    /**
     * Test transform with composite primary key and less number of DB props.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testTransformWithCompositePrimaryKeyAndLessNumberOfDBProps() throws IOException, SQLException {
        try {
            byte[] employee =
                    IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeWithProp.odata"));
            OData definition = ODataSynchronizer.parseOData("/transformers/EmployeeWithProp.odata", employee);

            Table model = new Table("EMPLOYEES");
            new TableColumn("COMPANY_ID", "Edm.Int32", "0", true, true, model);
            new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", "0", true, true, model);

            model.setKind(ISqlKeywords.METADATA_TABLE);
            when(odataDatabaseMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);

            new OData2ODataXTransformer(defaultTableMetadataProvider).transform(definition);
        } catch (Exception e) {
            assertTrue(e instanceof OData2TransformerException);
        }
    }

    /**
     * Test transform with annotations.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testTransformWithAnnotations() throws IOException, SQLException {
        byte[] employee = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/Employee.odata"));
        OData definition = ODataSynchronizer.parseOData("/transformers/Employee.odata", employee);

        definition.getEntities()
                  .get(0)
                  .getAnnotationsEntitySet()
                  .put("sap:creatable", "true");
        definition.getEntities()
                  .get(0)
                  .getAnnotationsEntitySet()
                  .put("sap:updatable-path", "Updatable");
        definition.getEntities()
                  .get(0)
                  .getAnnotationsEntityType()
                  .put("sap:semantics", "aggregate");

        definition.getEntities()
                  .get(0)
                  .getProperties()
                  .get(0)
                  .getAnnotationsProperty()
                  .put("sap:label", "someLabel");
        definition.getEntities()
                  .get(0)
                  .getProperties()
                  .get(0)
                  .getAnnotationsProperty()
                  .put("sap:aggregation-role", "dimension");
        definition.getEntities()
                  .get(0)
                  .getNavigations()
                  .get(0)
                  .getAnnotationsNavigationProperty()
                  .put("sap:filterable", "false");

        definition.getAssociations()
                  .get(0)
                  .getAnnotationsAssociationSet()
                  .put("sap:creatable", "true");

        Table model = new Table("EMPLOYEES");
        new TableColumn("COMPANY_ID", "Edm.Int32", "0", true, true, model);
        new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", "0", true, true, model);

        model.setKind(ISqlKeywords.METADATA_TABLE);
        when(odataDatabaseMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);

        model = new Table("PHONES");
        new TableColumn("NUMBER", "Edm.Int32", "0", true, true, model);
        new TableColumn("FK_COMPANY_ID", "Edm.Int32", "0", model);
        new TableColumn("FK_EMPLOYEE_NUMBER", "Edm.Int32", "0", model);

        model.setKind(ISqlKeywords.METADATA_TABLE);
        when(odataDatabaseMetadataUtil.getTableMetadata("PHONES", null)).thenReturn(model);

        String entitySchema = "<Schema Namespace=\"np\"\n" + "\txmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\">\n"
                + "\t<EntityType Name=\"employeeType\" sap:semantics=\"aggregate\">\n" + "\t\t<Key>\n"
                + "\t\t\t<PropertyRef Name=\"companyId\" />\n" + "\t\t\t<PropertyRef Name=\"employeeNumber\" />\n" + "\t\t</Key>\n"
                + "\t\t<Property Name=\"companyId\" Nullable=\"false\" Type=\"Edm.Int32\" sap:label=\"someLabel\" sap:aggregation-role=\"dimension\"/>\n"
                + "\t\t<Property Name=\"employeeNumber\" Nullable=\"false\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<NavigationProperty Name=\"HisPhones\" Relationship=\"np.Employees_PhonesType\" FromRole=\"employeePrincipal\" ToRole=\"phoneDependent\" sap:filterable=\"false\"/>\n"
                + "\t</EntityType>\n" + "\t<EntityType Name=\"phoneType\">\n" + "\t\t<Key>\n" + "\t\t\t<PropertyRef Name=\"number\" />\n"
                + "\t\t</Key>\n" + "\t\t<Property Name=\"number\" Nullable=\"false\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"fkCompanyId\" Nullable=\"false\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"fkEmployeeNumber\" Nullable=\"false\" Type=\"Edm.Int32\"/>\n" + "\t</EntityType>\n"
                + "\t<Association Name=\"Employees_PhonesType\">\n"
                + "\t\t<End Type=\"np.employeeType\" Role=\"employeePrincipal\" Multiplicity=\"1\"/>\n"
                + "\t\t<End Type=\"np.phoneType\" Role=\"phoneDependent\" Multiplicity=\"*\"/>\n" + " \t</Association>\n" + "</Schema>\n";
        String entitySet =
                "\t\t<EntitySet Name=\"Employees\" EntityType=\"np.employeeType\" sap:creatable=\"true\" sap:updatable-path=\"Updatable\"/>\n"
                        + "\t\t<EntitySet Name=\"Phones\" EntityType=\"np.phoneType\"/>\n"
                        + "\t<AssociationSet Name=\"Employees_Phones\" Association=\"np.Employees_PhonesType\" sap:creatable=\"true\">\n"
                        + "\t\t\t<End Role=\"employeePrincipal\" EntitySet=\"Employees\"/>\n"
                        + " \t\t\t<End Role=\"phoneDependent\" EntitySet=\"Phones\"/>\n" + "\t\t\t</AssociationSet>\n";

        String[] actualResult = new OData2ODataXTransformer(defaultTableMetadataProvider).transform(definition);
        assertArrayEquals(new String[] {entitySchema, entitySet}, actualResult);
    }

    /**
     * Test transform on view with gen id.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testTransformOnViewWithGenId() throws IOException, SQLException {
        byte[] employee = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeView.odata"));
        OData definition = ODataSynchronizer.parseOData("/transformers/EmployeeView.odata", employee);
        definition.getEntities()
                  .get(0)
                  .getKeys()
                  .add("GEN_ID");

        Table model = new Table("EMPLOYEES");
        new TableColumn("COMPANY_ID", "Edm.Int32", "0", true, true, model);
        new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", "0", true, true, model);
        new TableColumn("ORDER_ID", "Edm.Int32", "0", model);
        new TableColumn("ADDRESS_ID", "Edm.Int32", "0", model);

        model.setKind(ISqlKeywords.METADATA_VIEW);
        when(odataDatabaseMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);

        String entitySchema = "<Schema Namespace=\"np\"\n" + "\txmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\">\n"
                + "\t<EntityType Name=\"employeeType\">\n" + "\t\t<Key>\n" + "\t\t\t<PropertyRef Name=\"GEN_ID\" />\n" + "\t\t</Key>\n"
                + "\t\t<Property Name=\"GEN_ID\" Type=\"Edm.String\" Nullable=\"false\" MaxLength=\"2147483647\" sap:filterable=\"false\"/>\n"
                + "\t\t<Property Name=\"CompanyId\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"EmployeeNumber\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"OrderId\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"AddressId\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n" + "\t</EntityType>\n" + "</Schema>\n";
        String entitySet = "\t\t<EntitySet Name=\"Employees\" EntityType=\"np.employeeType\"/>\n";

        String[] actualResult = new OData2ODataXTransformer(defaultTableMetadataProvider).transform(definition);
        assertArrayEquals(new String[] {entitySchema, entitySet}, actualResult);
    }

    /**
     * Test transform on view with original keys.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testTransformOnViewWithOriginalKeys() throws IOException, SQLException {
        byte[] employee = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeView.odata"));
        OData definition = ODataSynchronizer.parseOData("/transformers/EmployeeView.odata", employee);
        definition.getEntities()
                  .get(0)
                  .getKeys()
                  .add("COMPANY_ID");
        definition.getEntities()
                  .get(0)
                  .getKeys()
                  .add("EMPLOYEE_NUMBER");

        Table model = new Table("EMPLOYEES");
        new TableColumn("COMPANY_ID", "Edm.Int32", "0", true, true, model);
        new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", "0", true, true, model);
        new TableColumn("ORDER_ID", "Edm.Int32", "0", model);
        new TableColumn("ADDRESS_ID", "Edm.Int32", "0", model);

        model.setKind(ISqlKeywords.METADATA_VIEW);
        when(odataDatabaseMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);

        String entitySchema = "<Schema Namespace=\"np\"\n" + "\txmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\">\n"
                + "\t<EntityType Name=\"employeeType\">\n" + "\t\t<Key>\n" + "\t\t\t<PropertyRef Name=\"CompanyId\" />\n"
                + "\t\t\t<PropertyRef Name=\"EmployeeNumber\" />\n" + "\t\t</Key>\n"
                + "\t\t<Property Name=\"CompanyId\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"EmployeeNumber\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"OrderId\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"AddressId\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n" + "\t</EntityType>\n" + "</Schema>\n";
        String entitySet = "\t\t<EntitySet Name=\"Employees\" EntityType=\"np.employeeType\"/>\n";

        String[] actualResult = new OData2ODataXTransformer(defaultTableMetadataProvider).transform(definition);
        assertArrayEquals(new String[] {entitySchema, entitySet}, actualResult);
    }

    /**
     * Test transform on view with props and original keys.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testTransformOnViewWithPropsAndOriginalKeys() throws IOException, SQLException {
        byte[] employee = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeView.odata"));
        OData definition = ODataSynchronizer.parseOData("/transformers/EmployeeView.odata", employee);
        definition.getEntities()
                  .get(0)
                  .getKeys()
                  .add("COMPANY_ID");
        definition.getEntities()
                  .get(0)
                  .getKeys()
                  .add("EMPLOYEE_NUMBER");

        definition.getEntities()
                  .get(0)
                  .getProperties()
                  .add(new ODataProperty().setName("myORDER_ID")
                                          .setType("Edm.Int32")
                                          .setNullable(false)
                                          .setColumn("ORDER_ID"));
        definition.getEntities()
                  .get(0)
                  .getProperties()
                  .add(new ODataProperty().setName("myCOMPANY_ID")
                                          .setType("Edm.Int32")
                                          .setNullable(false)
                                          .setColumn("COMPANY_ID"));
        definition.getEntities()
                  .get(0)
                  .getProperties()
                  .add(new ODataProperty().setName("myEMPLOYEE_NUMBER")
                                          .setType("Edm.Int32")
                                          .setNullable(false)
                                          .setColumn("EMPLOYEE_NUMBER"));

        Table model = new Table("EMPLOYEES");
        new TableColumn("COMPANY_ID", "Edm.Int32", "0", true, true, model);
        new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", "0", true, true, model);
        new TableColumn("ORDER_ID", "Edm.Int32", "0", model);
        new TableColumn("ADDRESS_ID", "Edm.Int32", "0", model);

        model.setKind(ISqlKeywords.METADATA_VIEW);
        when(odataDatabaseMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);

        String entitySchema = "<Schema Namespace=\"np\"\n" + "\txmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\">\n"
                + "\t<EntityType Name=\"employeeType\">\n" + "\t\t<Key>\n" + "\t\t\t<PropertyRef Name=\"myCOMPANY_ID\" />\n"
                + "\t\t\t<PropertyRef Name=\"myEMPLOYEE_NUMBER\" />\n" + "\t\t</Key>\n"
                + "\t\t<Property Name=\"myORDER_ID\" Nullable=\"false\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"myCOMPANY_ID\" Nullable=\"false\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"myEMPLOYEE_NUMBER\" Nullable=\"false\" Type=\"Edm.Int32\"/>\n" + "\t</EntityType>\n"
                + "</Schema>\n";
        String entitySet = "\t\t<EntitySet Name=\"Employees\" EntityType=\"np.employeeType\"/>\n";

        String[] actualResult = new OData2ODataXTransformer(defaultTableMetadataProvider).transform(definition);
        assertArrayEquals(new String[] {entitySchema, entitySet}, actualResult);
    }

    /**
     * Test transform on view with not original keys.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testTransformOnViewWithNotOriginalKeys() throws IOException, SQLException {
        byte[] employee = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeView.odata"));
        OData definition = ODataSynchronizer.parseOData("/transformers/EmployeeView.odata", employee);
        definition.getEntities()
                  .get(0)
                  .getKeys()
                  .add("ADDRESS_ID");

        Table model = new Table("EMPLOYEES");
        new TableColumn("COMPANY_ID", "Edm.Int32", "0", true, true, model);
        new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", "0", true, true, model);
        new TableColumn("ORDER_ID", "Edm.Int32", "0", model);
        new TableColumn("ADDRESS_ID", "Edm.Int32", "0", model);
        new TableColumn("ID", "Edm.Int32", "0", model);

        model.setKind(ISqlKeywords.METADATA_VIEW);
        when(odataDatabaseMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);

        String entitySchema = "<Schema Namespace=\"np\"\n" + "\txmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\">\n"
                + "\t<EntityType Name=\"employeeType\">\n" + "\t\t<Key>\n" + "\t\t\t<PropertyRef Name=\"AddressId\" />\n" + "\t\t</Key>\n"
                + "\t\t<Property Name=\"CompanyId\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"EmployeeNumber\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"OrderId\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"AddressId\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"Id\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n" + "\t</EntityType>\n" + "</Schema>\n";
        String entitySet = "\t\t<EntitySet Name=\"Employees\" EntityType=\"np.employeeType\"/>\n";

        String[] actualResult = new OData2ODataXTransformer(defaultTableMetadataProvider).transform(definition);
        assertArrayEquals(new String[] {entitySchema, entitySet}, actualResult);
    }

    /**
     * Test transform on view with props and not original keys.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testTransformOnViewWithPropsAndNotOriginalKeys() throws IOException, SQLException {
        byte[] employee = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeView.odata"));
        OData definition = ODataSynchronizer.parseOData("/transformers/EmployeeView.odata", employee);
        definition.getEntities()
                  .get(0)
                  .getKeys()
                  .add("ADDRESS_ID");

        definition.getEntities()
                  .get(0)
                  .getProperties()
                  .add(new ODataProperty().setName("myORDER_ID")
                                          .setType("Edm.Int32")
                                          .setNullable(false)
                                          .setColumn("ORDER_ID"));
        definition.getEntities()
                  .get(0)
                  .getProperties()
                  .add(new ODataProperty().setName("myADDRESS_ID")
                                          .setType("Edm.Int32")
                                          .setNullable(false)
                                          .setColumn("ADDRESS_ID"));

        Table model = new Table("EMPLOYEES");
        new TableColumn("COMPANY_ID", "Edm.Int32", "0", true, true, model);
        new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", "0", true, true, model);
        new TableColumn("ORDER_ID", "Edm.Int32", "0", model);
        new TableColumn("ADDRESS_ID", "Edm.Int32", "0", model);

        model.setKind(ISqlKeywords.METADATA_VIEW);
        when(odataDatabaseMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);

        String entitySchema = "<Schema Namespace=\"np\"\n" + "\txmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\">\n"
                + "\t<EntityType Name=\"employeeType\">\n" + "\t\t<Key>\n" + "\t\t\t<PropertyRef Name=\"myADDRESS_ID\" />\n"
                + "\t\t</Key>\n" + "\t\t<Property Name=\"myORDER_ID\" Nullable=\"false\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"myADDRESS_ID\" Nullable=\"false\" Type=\"Edm.Int32\"/>\n" + "\t</EntityType>\n" + "</Schema>\n";
        String entitySet = "\t\t<EntitySet Name=\"Employees\" EntityType=\"np.employeeType\"/>\n";

        String[] actualResult = new OData2ODataXTransformer(defaultTableMetadataProvider).transform(definition);
        assertArrayEquals(new String[] {entitySchema, entitySet}, actualResult);
    }

    /**
     * Test transform on view with non existing props and not original keys.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testTransformOnViewWithNonExistingPropsAndNotOriginalKeys() throws IOException, SQLException {
        try {
            byte[] employee = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeView.odata"));
            OData definition = ODataSynchronizer.parseOData("/transformers/EmployeeView.odata", employee);
            definition.getEntities()
                      .get(0)
                      .getKeys()
                      .add("ADDRESS_ID");

            definition.getEntities()
                      .get(0)
                      .getProperties()
                      .add(new ODataProperty().setName("myERROR_ID")
                                              .setType("Edm.Int32")
                                              .setNullable(false)
                                              .setColumn("ERROR_ID"));
            definition.getEntities()
                      .get(0)
                      .getProperties()
                      .add(new ODataProperty().setName("myADDRESS_ID")
                                              .setType("Edm.Int32")
                                              .setNullable(false)
                                              .setColumn("ADDRESS_ID"));

            Table model = new Table("EMPLOYEES");
            new TableColumn("COMPANY_ID", "Edm.Int32", "0", true, true, model);
            new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", "0", true, true, model);
            new TableColumn("ORDER_ID", "Edm.Int32", "0", model);
            new TableColumn("ADDRESS_ID", "Edm.Int32", "0", model);

            model.setKind(ISqlKeywords.METADATA_VIEW);
            when(odataDatabaseMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);

            new OData2ODataXTransformer(defaultTableMetadataProvider).transform(definition);
        } catch (Exception e) {
            assertTrue(e instanceof OData2TransformerException);
        }
    }

    /**
     * Test transform on hana calculation view with props and parameters.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testTransformOnHanaCalculationViewWithPropsAndParameters() throws IOException, SQLException {
        byte[] employee = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeView.odata"));
        OData definition = ODataSynchronizer.parseOData("/transformers/EmployeeView.odata", employee);

        definition.getEntities()
                  .get(0)
                  .getKeys()
                  .add("COMPANY_ID");
        definition.getEntities()
                  .get(0)
                  .getKeys()
                  .add("EMPLOYEE_NUMBER");

        definition.getEntities()
                  .get(0)
                  .getProperties()
                  .add(new ODataProperty().setName("myORDER_ID")
                                          .setType("Edm.Int32")
                                          .setNullable(false)
                                          .setColumn("ORDER_ID"));
        definition.getEntities()
                  .get(0)
                  .getProperties()
                  .add(new ODataProperty().setName("myCOMPANY_ID")
                                          .setType("Edm.Int32")
                                          .setNullable(false)
                                          .setColumn("COMPANY_ID"));
        definition.getEntities()
                  .get(0)
                  .getProperties()
                  .add(new ODataProperty().setName("myEMPLOYEE_NUMBER")
                                          .setType("Edm.Int32")
                                          .setNullable(false)
                                          .setColumn("EMPLOYEE_NUMBER"));

        definition.getEntities()
                  .get(0)
                  .getParameters()
                  .add(new ODataParameter().setName("isEmployeeFrom")
                                           .setType("Edm.DateTime")
                                           .setNullable(true));

        Table model = new Table("EMPLOYEES");
        new TableColumn("COMPANY_ID", "Edm.Int32", "0", true, true, model);
        new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", "0", true, true, model);
        new TableColumn("ORDER_ID", "Edm.Int32", "0", model);

        model.setKind(ISqlKeywords.METADATA_CALC_VIEW);
        when(odataDatabaseMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);

        String entitySchema = "<Schema Namespace=\"np\"\n" + "\txmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\">\n"
                + "\t<EntityType Name=\"employeeType\">\n" + "\t\t<Key>\n" + "\t\t\t<PropertyRef Name=\"isEmployeeFrom\" />\n"
                + "\t\t\t<PropertyRef Name=\"myCOMPANY_ID\" />\n" + "\t\t\t<PropertyRef Name=\"myEMPLOYEE_NUMBER\" />\n" + "\t\t</Key>\n"
                + "\t\t<Property Name=\"isEmployeeFrom\" Nullable=\"true\" Type=\"Edm.DateTime\"/>\n"
                + "\t\t<Property Name=\"myORDER_ID\" Nullable=\"false\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"myCOMPANY_ID\" Nullable=\"false\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"myEMPLOYEE_NUMBER\" Nullable=\"false\" Type=\"Edm.Int32\"/>\n" + "\t</EntityType>\n"
                + "</Schema>\n";
        String entitySet = "\t\t<EntitySet Name=\"Employees\" EntityType=\"np.employeeType\"/>\n";

        String[] actualResult = new OData2ODataXTransformer(defaultTableMetadataProvider).transform(definition);
        assertArrayEquals(new String[] {entitySchema, entitySet}, actualResult);
    }

    /**
     * Test transform with unsupported object type.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testTransformWithUnsupportedObjectType() throws IOException, SQLException {
        byte[] employee = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeView.odata"));
        OData definition = ODataSynchronizer.parseOData("/transformers/EmployeeView.odata", employee);

        Table model = new Table("EMPLOYEES");
        new TableColumn("COMPANY_ID", "Edm.Int32", "0", true, true, model);
        new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", "0", true, true, model);
        new TableColumn("ORDER_ID", "Edm.Int32", "0", model);
        new TableColumn("ADDRESS_ID", "Edm.Int32", "0", model);

        model.setKind(ISqlKeywords.METADATA_SYSTEM_TABLE);
        when(odataDatabaseMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);
        String entitySchema = "<Schema Namespace=\"np\"\n" + "\txmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\">\n" + "</Schema>\n";
        String entitySet = "";

        String[] actualResult = new OData2ODataXTransformer(defaultTableMetadataProvider).transform(definition);
        assertArrayEquals(new String[] {entitySchema, entitySet}, actualResult);
    }

    /**
     * Test transform entity property.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testTransformEntityProperty() throws IOException, SQLException {
        OData definition = OData2ODataTransformerTestUtil.loadData_testTransformEntityProperty(odataDatabaseMetadataUtil);

        String entitySchema = "<Schema Namespace=\"mytest\"\n" + "\txmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\">\n"
                + "\t<EntityType Name=\"Entity1Type\">\n" + "\t\t<Key>\n" + "\t\t\t<PropertyRef Name=\"entity1Id\" />\n" + "\t\t</Key>\n"
                + "\t\t<Property Name=\"entity1Id\" Nullable=\"false\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"property2\" Nullable=\"true\" Type=\"Edm.String\"/>\n"
                + "\t\t<Property Name=\"property3\" Nullable=\"true\" Type=\"Edm.String\"/>\n"
                + "\t\t<Property Name=\"Country.Id\" Nullable=\"true\" Type=\"Edm.String\"/>\n"
                + "\t\t<NavigationProperty Name=\"Entity2\" Relationship=\"mytest.Entity1Entity2Type\" FromRole=\"Entity1Principal\" ToRole=\"Entity2Dependent\"/>\n"
                + "\t</EntityType>\n" + "\t<EntityType Name=\"Entity2Type\">\n" + "\t\t<Key>\n"
                + "\t\t\t<PropertyRef Name=\"entity2Id\" />\n" + "\t\t</Key>\n"
                + "\t\t<Property Name=\"entity2Id\" Nullable=\"false\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"property2\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"property3\" Nullable=\"true\" Type=\"Edm.String\"/>\n"
                + "\t\t<Property Name=\"property4_5\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"Entity1entity1Id\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n" + "\t</EntityType>\n"
                + "\t<EntityType Name=\"Entity3Type\">\n" + "\t\t<Key>\n" + "\t\t\t<PropertyRef Name=\"Entity3_id\" />\n" + "\t\t</Key>\n"
                + "\t\t<Property Name=\"Entity3_id\" Nullable=\"true\" Type=\"Edm.Int32\"/>\n"
                + "\t\t<Property Name=\"Name_id\" Nullable=\"true\" Type=\"Edm.String\"/>\n" + "\t</EntityType>\n"
                + "\t<Association Name=\"Entity1Entity2Type\">\n"
                + "\t\t<End Type=\"mytest.Entity1Type\" Role=\"Entity1Principal\" Multiplicity=\"1\"/>\n"
                + "\t\t<End Type=\"mytest.Entity2Type\" Role=\"Entity2Dependent\" Multiplicity=\"*\"/>\n" + " \t</Association>\n"
                + "</Schema>\n";
        String entitySet = "\t\t<EntitySet Name=\"Entity1\" EntityType=\"mytest.Entity1Type\"/>\n"
                + "\t\t<EntitySet Name=\"Entity2\" EntityType=\"mytest.Entity2Type\"/>\n"
                + "\t\t<EntitySet Name=\"Entity3\" EntityType=\"mytest.Entity3Type\"/>\n"
                + "\t<AssociationSet Name=\"Entity1Entity2\" Association=\"mytest.Entity1Entity2Type\">\n"
                + "\t\t\t<End Role=\"Entity1Principal\" EntitySet=\"Entity1\"/>\n"
                + " \t\t\t<End Role=\"Entity2Dependent\" EntitySet=\"Entity2\"/>\n" + "\t\t\t</AssociationSet>\n";

        String[] actualResult = new OData2ODataXTransformer(defaultTableMetadataProvider).transform(definition);
        assertArrayEquals(new String[] {entitySchema, entitySet}, actualResult);
    }
}
