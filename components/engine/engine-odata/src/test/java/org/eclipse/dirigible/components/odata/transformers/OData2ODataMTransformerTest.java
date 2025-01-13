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

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.domain.TableColumn;
import org.eclipse.dirigible.components.data.structures.domain.TableConstraintForeignKey;
import org.eclipse.dirigible.components.odata.domain.OData;
import org.eclipse.dirigible.components.odata.factory.ODataDefinitionFactoryTest;
import org.eclipse.dirigible.components.odata.synchronizer.ODataSynchronizer;
import org.eclipse.dirigible.database.sql.ISqlKeywords;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.when;

/**
 * The Class OData2ODataMTransformerTest.
 */
@ExtendWith(MockitoExtension.class)
public class OData2ODataMTransformerTest {

    /** The db metadata util. */
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ODataDatabaseMetadataUtil odataDatabaseMetadataUtil;

    /** The default table metadata provider. */
    @InjectMocks
    private DefaultTableMetadataProvider defaultTableMetadataProvider;

    /**
     * Test transform orders.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testTransformOrders() throws IOException, SQLException {
        byte[] orders = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/orders/Orders.odata"));
        OData definition = ODataSynchronizer.parseOData("/orders/Orders.odata", orders);

        Table model = new Table("ORDERS");
        new TableColumn("Id", "Edm.Int32", "0", true, true, model);
        new TableColumn("Customer", "Edm.String", "20", model);

        when(odataDatabaseMetadataUtil.getTableMetadata("ORDERS", null)).thenReturn(model);

        model = new Table("ITEMS");
        new TableColumn("Id", "Edm.Int32", "0", true, true, model);
        new TableColumn("OrderId", "Edm.Int32", "0", model);
        new TableConstraintForeignKey("ORDERS", null, "OrderId", "Id", model.getConstraints());

        when(odataDatabaseMetadataUtil.getTableMetadata("ITEMS", null)).thenReturn(model);

        String entityOrder =
                "{\n" + "\t\"edmType\": \"OrderType\",\n" + "\t\"edmTypeFqn\": \"org.apache.olingo.odata2.ODataOrders.OrderType\",\n"
                        + "\t\"sqlTable\": \"ORDERS\",\n" + "\t\"dataStructureType\": \"TABLE\",\n" + "\t\"sqlSchema\": null,\n"
                        + "\t\"Id\": \"Id\",\n" + "\t\"Customer\": \"Customer\",\n" + "\t\"_ref_ItemType\": {\n"
                        + "\t\t\"joinColumn\" : [\n" + "\t\t\t\"Id\"\n" + "\t\t]\n" + "\t},\n" + "\t\"_pk_\" : \"Id\"\n" + "}";
        String entityItem =
                "{\n" + "\t\"edmType\": \"ItemType\",\n" + "\t\"edmTypeFqn\": \"org.apache.olingo.odata2.ODataOrders.ItemType\",\n"
                        + "\t\"sqlTable\": \"ITEMS\",\n" + "\t\"dataStructureType\": \"TABLE\",\n" + "\t\"sqlSchema\": null,\n"
                        + "\t\"Id\": \"Id\",\n" + "\t\"Orderid\": \"OrderId\",\n" + "\t\"_ref_OrderType\": {\n" + "\t\t\"joinColumn\" : [\n"
                        + "\t\t\t\"OrderId\"\n" + "\t\t]\n" + "\t},\n" + "\t\"_pk_\" : \"Id\"\n" + "}";
        String[] transformed =
                new OData2ODataMTransformer(defaultTableMetadataProvider, new DefaultPropertyNameEscaper()).transform(definition);
        assertArrayEquals(new String[] {entityOrder, entityItem}, transformed);
    }

    /**
     * Test transform orders case sensitive.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testTransformOrdersCaseSensitive() throws IOException, SQLException {
        byte[] orders = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/orderscs/Orders.odata"));
        OData definition = ODataSynchronizer.parseOData("/orderscs/Orders.odata", orders);

        Table model = new Table("ORDERS");
        new TableColumn("ID", "Edm.Int32", "0", true, true, model);
        new TableColumn("CUSTOMER", "Edm.String", "20", model);

        when(odataDatabaseMetadataUtil.getTableMetadata("ORDERS", null)).thenReturn(model);

        model = new Table("ITEMS");
        new TableColumn("ITEM_ID", "Edm.Int32", "0", true, true, model);
        new TableColumn("ORDER_ID", "Edm.Int32", "0", model);
        new TableConstraintForeignKey("ORDERS", null, "ORDER_ID", "ID", model.getConstraints());

        when(odataDatabaseMetadataUtil.getTableMetadata("ITEMS", null)).thenReturn(model);

        String entityOrder =
                "{\n" + "\t\"edmType\": \"OrderType\",\n" + "\t\"edmTypeFqn\": \"org.apache.olingo.odata2.ODataOrders.OrderType\",\n"
                        + "\t\"sqlTable\": \"ORDERS\",\n" + "\t\"dataStructureType\": \"TABLE\",\n" + "\t\"sqlSchema\": null,\n"
                        + "\t\"Id\": \"ID\",\n" + "\t\"Customer\": \"CUSTOMER\",\n" + "\t\"_ref_ItemType\": {\n"
                        + "\t\t\"joinColumn\" : [\n" + "\t\t\t\"ID\"\n" + "\t\t]\n" + "\t},\n" + "\t\"_pk_\" : \"ID\"\n" + "}";
        String entityItem =
                "{\n" + "\t\"edmType\": \"ItemType\",\n" + "\t\"edmTypeFqn\": \"org.apache.olingo.odata2.ODataOrders.ItemType\",\n"
                        + "\t\"sqlTable\": \"ITEMS\",\n" + "\t\"dataStructureType\": \"TABLE\",\n" + "\t\"sqlSchema\": null,\n"
                        + "\t\"ItemId\": \"ITEM_ID\",\n" + "\t\"OrderId\": \"ORDER_ID\",\n" + "\t\"_ref_OrderType\": {\n"
                        + "\t\t\"joinColumn\" : [\n" + "\t\t\t\"ORDER_ID\"\n" + "\t\t]\n" + "\t},\n" + "\t\"_pk_\" : \"ITEM_ID\"\n" + "}";
        String[] transformed =
                new OData2ODataMTransformer(defaultTableMetadataProvider, new DefaultPropertyNameEscaper()).transform(definition);
        assertArrayEquals(new String[] {entityOrder, entityItem}, transformed);
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

        String entity1 = "{\n" + "\t\"edmType\": \"Entity1Type\",\n" + "\t\"edmTypeFqn\": \"mytest.Entity1Type\",\n"
                + "\t\"sqlTable\": \"ENTITY1\",\n" + "\t\"dataStructureType\": \"TABLE\",\n" + "\t\"sqlSchema\": null,\n"
                + "\t\"entity1Id\": \"ENTITY1ID\",\n" + "\t\"property2\": \"PROPERTY2\",\n" + "\t\"property3\": \"PROPERTY3\",\n"
                + "\t\"Country_Id\": \"Country.Id\",\n" + "\t\"_ref_Entity2Type\": {\n" + "\t\t\"joinColumn\" : [\n"
                + "\t\t\t\"ENTITY1ID\"\n" + "\t\t]\n" + "\t},\n" + "\t\"_pk_\" : \"ENTITY1ID\"\n" + "}";
        String entity2 = "{\n" + "\t\"edmType\": \"Entity2Type\",\n" + "\t\"edmTypeFqn\": \"mytest.Entity2Type\",\n"
                + "\t\"sqlTable\": \"ENTITY2\",\n" + "\t\"dataStructureType\": \"TABLE\",\n" + "\t\"sqlSchema\": null,\n"
                + "\t\"entity2Id\": \"ENTITY2ID\",\n" + "\t\"property2\": \"PROPERTY2\",\n" + "\t\"property3\": \"PROPERTY3\",\n"
                + "\t\"property4_5\": \"PROPERTY4_5\",\n" + "\t\"Entity1entity1Id\": \"ENTITY1ENTITY1ID\",\n"
                + "\t\"_ref_Entity1Type\": {\n" + "\t\t\"joinColumn\" : [\n" + "\t\t\t\"ENTITY1ENTITY1ID\"\n" + "\t\t]\n" + "\t},\n"
                + "\t\"_pk_\" : \"ENTITY2ID\"\n" + "}";
        String entity3 = "{\n" + "\t\"edmType\": \"Entity3Type\",\n" + "\t\"edmTypeFqn\": \"mytest.Entity3Type\",\n"
                + "\t\"sqlTable\": \"ENTITY3\",\n" + "\t\"dataStructureType\": \"TABLE\",\n" + "\t\"sqlSchema\": null,\n"
                + "\t\"Entity3_id\": \"ENTITY3.ID\",\n" + "\t\"Name_id\": \"NAME.ID\",\n" + "\t\"_pk_\" : \"ENTITY3.ID\"\n" + "}";

        String[] transformed =
                new OData2ODataMTransformer(defaultTableMetadataProvider, new DefaultPropertyNameEscaper()).transform(definition);
        assertArrayEquals(new String[] {entity1, entity2, entity3}, transformed);
    }

    /**
     * Test transform entity with parameters when hana calculation view.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testTransformEntityWithParametersWhenHanaCalculationView() throws IOException, SQLException {
        byte[] employee =
                IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeWithParameters.odata"));
        OData definition = ODataSynchronizer.parseOData("/transformers/EmployeeWithParameters.odata", employee);

        Table model = new Table("EMPLOYEES");
        new TableColumn("COMPANY_ID", "Edm.Int32", "0", true, true, model);
        new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", "0", true, true, model);

        model.setKind("CALC VIEW");
        when(odataDatabaseMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);

        String entityEmployee = "{\n" + "\t\"edmType\": \"employeeType\",\n" + "\t\"edmTypeFqn\": \"np.employeeType\",\n"
                + "\t\"sqlTable\": \"EMPLOYEES\",\n" + "\t\"dataStructureType\": \"CALC VIEW\",\n" + "\t\"sqlSchema\": null,\n"
                + "\t\"companyId\": \"COMPANY_ID\",\n" + "\t\"employeeNumber\": \"EMPLOYEE_NUMBER\",\n"
                + "\t\"EmployeeId\": \"EmployeeId\",\n" + "\t\"_parameters_\" : [\"EmployeeId\"],\n"
                + "\t\"_pk_\" : \"COMPANY_ID,EMPLOYEE_NUMBER\"\n" + "}";

        String[] actualResult =
                new OData2ODataMTransformer(defaultTableMetadataProvider, new DefaultPropertyNameEscaper()).transform(definition);
        assertArrayEquals(new String[] {entityEmployee}, actualResult);
    }

    /**
     * Test transform with composite primary key and properties.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testTransformWithCompositePrimaryKeyAndProperties() throws IOException, SQLException {
        byte[] employee = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/Employee.odata"));
        OData definition = ODataSynchronizer.parseOData("/transformers/Employee.odata", employee);

        Table model = new Table("EMPLOYEES");
        new TableColumn("COMPANY_ID", "Edm.Int32", "0", true, true, model);
        new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", "0", true, true, model);

        when(odataDatabaseMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);

        model = new Table("PHONES");
        new TableColumn("NUMBER", "Edm.Int32", "0", true, true, model);
        new TableColumn("FK_COMPANY_ID", "Edm.Int32", "0", model);
        new TableColumn("FK_EMPLOYEE_NUMBER", "Edm.Int32", "0", model);

        when(odataDatabaseMetadataUtil.getTableMetadata("PHONES", null)).thenReturn(model);

        String entityEmployee = "{\n" + "\t\"edmType\": \"employeeType\",\n" + "\t\"edmTypeFqn\": \"np.employeeType\",\n"
                + "\t\"sqlTable\": \"EMPLOYEES\",\n" + "\t\"dataStructureType\": \"TABLE\",\n" + "\t\"sqlSchema\": null,\n"
                + "\t\"companyId\": \"COMPANY_ID\",\n" + "\t\"employeeNumber\": \"EMPLOYEE_NUMBER\",\n" + "\t\"_ref_phoneType\": {\n"
                + "\t\t\"joinColumn\" : [\n" + "\t\t\t\"COMPANY_ID\",\"EMPLOYEE_NUMBER\"\n" + "\t\t]\n" + "\t},\n"
                + "\t\"_pk_\" : \"COMPANY_ID,EMPLOYEE_NUMBER\"\n" + "}";
        String phoneEntity = "{\n" + "\t\"edmType\": \"phoneType\",\n" + "\t\"edmTypeFqn\": \"np.phoneType\",\n"
                + "\t\"sqlTable\": \"PHONES\",\n" + "\t\"dataStructureType\": \"TABLE\",\n" + "\t\"sqlSchema\": null,\n"
                + "\t\"number\": \"NUMBER\",\n" + "\t\"fkCompanyId\": \"FK_COMPANY_ID\",\n"
                + "\t\"fkEmployeeNumber\": \"FK_EMPLOYEE_NUMBER\",\n" + "\t\"_ref_employeeType\": {\n" + "\t\t\"joinColumn\" : [\n"
                + "\t\t\t\"FK_COMPANY_ID\",\"FK_EMPLOYEE_NUMBER\"\n" + "\t\t]\n" + "\t},\n" + "\t\"_pk_\" : \"NUMBER\"\n" + "}";
        String[] actualResult =
                new OData2ODataMTransformer(defaultTableMetadataProvider, new DefaultPropertyNameEscaper()).transform(definition);
        assertArrayEquals(new String[] {entityEmployee, phoneEntity}, actualResult);
    }

    /**
     * Test many to many mapping table transformation.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testManyToManyMappingTableTransformation() throws IOException, SQLException {
        byte[] users = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/users/Users.odata"));
        OData definition = ODataSynchronizer.parseOData("/users/Users.odata", users);

        Table model = new Table("CVUSER");
        new TableColumn("ID", "Edm.Int32", "0", true, true, model);
        new TableColumn("FIRSTNAME", "Edm.String", "20", model);

        when(odataDatabaseMetadataUtil.getTableMetadata("CVUSER", null)).thenReturn(model);

        model = new Table("CVGROUP");
        new TableColumn("ID", "Edm.Int32", "0", true, true, model);
        new TableColumn("FIRSTNAME", "Edm.String", "20", model);

        when(odataDatabaseMetadataUtil.getTableMetadata("CVGROUP", null)).thenReturn(model);

        String entityUser = "{\n" + "\t\"edmType\": \"UsersType\",\n"
                + "\t\"edmTypeFqn\": \"org.apache.olingo.odata2.ODataUsers.UsersType\",\n" + "\t\"sqlTable\": \"CVUSER\",\n"
                + "\t\"dataStructureType\": \"TABLE\",\n" + "\t\"sqlSchema\": null,\n" + "\t\"Id\": \"ID\",\n"
                + "\t\"Firstname\": \"FIRSTNAME\",\n" + "\t\"_ref_GroupsType\": {\n" + "\t\t\"joinColumn\" : [\n" + "\t\t\t\"ID\"\n"
                + "\t\t],\n" + "\t\t\"manyToManyMappingTable\" : {\n" + "\t\t\t\"mappingTableName\" : \"USERSTOGROUP\",\n"
                + "\t\t\t\"mappingTableJoinColumn\" : \"UserId\"\n" + "\t\t}\n" + "\t},\n" + "\t\"_pk_\" : \"ID\"\n" + "}";
        String entityGroup = "{\n" + "\t\"edmType\": \"GroupsType\",\n"
                + "\t\"edmTypeFqn\": \"org.apache.olingo.odata2.ODataUsers.GroupsType\",\n" + "\t\"sqlTable\": \"CVGROUP\",\n"
                + "\t\"dataStructureType\": \"TABLE\",\n" + "\t\"sqlSchema\": null,\n" + "\t\"Id\": \"ID\",\n"
                + "\t\"Firstname\": \"FIRSTNAME\",\n" + "\t\"_ref_UsersType\": {\n" + "\t\t\"joinColumn\" : [\n" + "\t\t\t\"ID\"\n"
                + "\t\t],\n" + "\t\t\"manyToManyMappingTable\" : {\n" + "\t\t\t\"mappingTableName\" : \"USERSTOGROUP\",\n"
                + "\t\t\t\"mappingTableJoinColumn\" : \"GroupId\"\n" + "\t\t}\n" + "\t},\n" + "\t\"_pk_\" : \"ID\"\n" + "}";
        String[] transformed =
                new OData2ODataMTransformer(defaultTableMetadataProvider, new DefaultPropertyNameEscaper()).transform(definition);
        assertArrayEquals(new String[] {entityUser, entityGroup}, transformed);
    }

    /**
     * Test view transformation.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testViewTransformation() throws IOException, SQLException {
        byte[] view = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/view/View.odata"));
        OData definition = ODataSynchronizer.parseOData("/view/View.odata", view);

        Table model = new Table("UserRole");
        new TableColumn("ZUSR_ROLE", "Edm.String", "20", model);
        new TableColumn("ZROLE_NAME", "Edm.String", "20", model);

        model.setKind(ISqlKeywords.KEYWORD_VIEW);
        when(odataDatabaseMetadataUtil.getTableMetadata("UserRole", null)).thenReturn(model);

        String entityView = "{\n" + "\t\"edmType\": \"UserRoleType\",\n"
                + "\t\"edmTypeFqn\": \"org.apache.olingo.odata2.ODataUserRole.UserRoleType\",\n" + "\t\"sqlTable\": \"UserRole\",\n"
                + "\t\"dataStructureType\": \"VIEW\",\n" + "\t\"sqlSchema\": null,\n" + "\t\"ZUSR_ROLE\": \"ZUSR_ROLE\",\n"
                + "\t\"ZROLE_NAME\": \"ZROLE_NAME\",\n" + "\t\"keyGenerated\": \"ID\",\n" + "\t\"_pk_\" : \"\"\n" + "}";

        String[] transformed =
                new OData2ODataMTransformer(defaultTableMetadataProvider, new DefaultPropertyNameEscaper()).transform(definition);
        assertArrayEquals(new String[] {entityView}, transformed);
    }

    /**
     * Test aggregations transformation.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testAggregationsTransformation() throws IOException, SQLException {
        byte[] customer = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/customer/Customer.odata"));
        OData definition = ODataSynchronizer.parseOData("/customer/Customer.odata", customer);

        Table model = new Table("CUSTOMER");
        new TableColumn("ID", "Edm.Int32", "0", false, true, model);
        new TableColumn("NUMBER", "Edm.Int32", "0", model);
        new TableColumn("PAYMENT", "Edm.Int32", "0", model);

        when(odataDatabaseMetadataUtil.getTableMetadata("CUSTOMER", null)).thenReturn(model);

        String aggregationEntity = "{\n" + "\t\"edmType\": \"CustomerType\",\n"
                + "\t\"edmTypeFqn\": \"org.apache.olingo.odata2.ODataCustomer.CustomerType\",\n" + "\t\"sqlTable\": \"CUSTOMER\",\n"
                + "\t\"dataStructureType\": \"TABLE\",\n" + "\t\"sqlSchema\": null,\n" + "\t\"ID\": \"ID\",\n"
                + "\t\"NUMBER\": \"NUMBER\",\n" + "\t\"PAYMENT\": \"PAYMENT\",\n" + "\t\"aggregationType\" : \"derived\",\n"
                + "\t\"aggregationProps\" : {\n" + "\t\t\"NUMBER\": \"SUM\",\n" + "\t\t\"PAYMENT\": \"AVERAGE\"\n" + "\t},\n"
                + "\t\"_pk_\" : \"ID\"\n" + "}";

        String[] transformed =
                new OData2ODataMTransformer(defaultTableMetadataProvider, new DefaultPropertyNameEscaper()).transform(definition);
        assertArrayEquals(new String[] {aggregationEntity}, transformed);
    }

    // public void testTransformWithCompositePrimaryKey() throws IOException, SQLException {
    // String employee =
    // IOUtils.toString(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeCompositePrimaryKey.odata"),
    // Charset.defaultCharset());
    // ODataDefinition definition =
    // ODataDefinitionFactory.parseOData("/transformers/EmployeeCompositePrimaryKey.odata", employee);
    //
    // TableColumn column1 = new TableColumn("COMPANY_ID", "Edm.Int32", true);
    // TableColumn column2 = new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", true);
    // Table model = new Table("EMPLOYEES", Arrays.asList(column1, column2), new ArrayList<>());
    // when(dbMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);
    //
    // TableColumn column7 = new TableColumn("ID", "Edm.Int32", true);
    // TableColumn column8 = new TableColumn("FK_PHONE", "Edm.Int32", false);
    // PersistenceTableRelationModel relPhone = new PersistenceTableRelationModel("ADDRESS", "PHONES",
    // "FK_PHONE", "ID", "CONSTRAINT_8C9F7", "CONSTRAINT_INDEX_E67");
    // model = new Table("ADDRESS", Arrays.asList(column7, column8),
    // Collections.singletonList(relPhone));
    // when(dbMetadataUtil.getTableMetadata("ADDRESS", null)).thenReturn(model);
    //
    // TableColumn column3 = new TableColumn("NUMBER", "Edm.Int32", true);
    // TableColumn column4 = new TableColumn("FK_COMPANY_ID", "Edm.Int32", false);
    // TableColumn column5 = new TableColumn("FK_EMPLOYEE_NUMBER", "Edm.Int32", false);
    // PersistenceTableRelationModel rel = new PersistenceTableRelationModel("PHONES", "EMPLOYEES",
    // "FK_COMPANY_ID", "COMPANY_ID", "CONSTRAINT_8C", "CONSTRAINT_INDEX_4");
    // PersistenceTableRelationModel rel2 = new PersistenceTableRelationModel("PHONES", "EMPLOYEES",
    // "FK_EMPLOYEE_NUMBER", "EMPLOYEE_NUMBER", "CONSTRAINT_8C9", "CONSTRAINT_INDEX_43");
    // model = new Table("PHONES", Arrays.asList(column3, column4, column5), Arrays.asList(rel, rel2));
    // when(dbMetadataUtil.getTableMetadata("PHONES", null)).thenReturn(model);
    //
    // String entityEmployee = "{\n" +
    // "\t\"edmType\": \"employeeType\",\n" +
    // "\t\"edmTypeFqn\": \"np.employeeType\",\n" +
    // "\t\"sqlTable\": \"EMPLOYEES\",\n" +
    // "\t\"CompanyId\": \"COMPANY_ID\",\n" +
    // "\t\"EmployeeNumber\": \"EMPLOYEE_NUMBER\",\n" +
    // "\t\"_ref_phoneType\": {\n" +
    // "\t\t\"joinColumn\" : [\n" +
    // "\t\t\t\"COMPANY_ID\",\"EMPLOYEE_NUMBER\"\n" +
    // "\t\t]\n" +
    // "\t},\n" +
    // "\t\"_pk_\" : \"COMPANY_ID,EMPLOYEE_NUMBER\"\n" +
    // "}";
    // String entityPhone = "{\n" +
    // "\t\"edmType\": \"phoneType\",\n" +
    // "\t\"edmTypeFqn\": \"np.phoneType\",\n" +
    // "\t\"sqlTable\": \"PHONES\",\n" +
    // "\t\"Number\": \"NUMBER\",\n" +
    // "\t\"FkCompanyId\": \"FK_COMPANY_ID\",\n" +
    // "\t\"FkEmployeeNumber\": \"FK_EMPLOYEE_NUMBER\",\n" +
    // "\t\"_ref_employeeType\": {\n" +
    // "\t\t\"joinColumn\" : [\n" +
    // "\t\t\t\"FK_COMPANY_ID\",\"FK_EMPLOYEE_NUMBER\"\n" +
    // "\t\t]\n" +
    // "\t},\n" +
    // "\t\"_ref_addressType\": {\n" +
    // "\t\t\"joinColumn\" : [\n" +
    // "\t\t\t\"NUMBER\"\n" +
    // "\t\t]\n" +
    // "\t},\n" +
    // "\t\"_pk_\" : \"NUMBER\"\n" +
    // "}";
    //
    // String entityAddress = "{\n" +
    // "\t\"edmType\": \"addressType\",\n" +
    // "\t\"edmTypeFqn\": \"np.addressType\",\n" +
    // "\t\"sqlTable\": \"ADDRESS\",\n" +
    // "\t\"Id\": \"ID\",\n" +
    // "\t\"FkPhone\": \"FK_PHONE\",\n" +
    // "\t\"_ref_phoneType\": {\n" +
    // "\t\t\"joinColumn\" : [\n" +
    // "\t\t\t\"FK_PHONE\"\n" +
    // "\t\t]\n" +
    // "\t},\n" +
    // "\t\"_pk_\" : \"ID\"\n" +
    // "}";
    // String[] actualResult = odata2ODataMTransformer.transform(definition);
    // assertArrayEquals(new String[]{entityEmployee, entityPhone, entityAddress}, actualResult);
    // }
    //
    // @Test
    // public void testTransformWithCompositePrimaryKeyWhenThereIsNoFK() throws IOException,
    // SQLException {
    // String employee =
    // IOUtils.toString(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeCompositePrimaryKey.odata"),
    // Charset.defaultCharset());
    // ODataDefinition definition =
    // ODataDefinitionFactory.parseOData("/transformers/EmployeeCompositePrimaryKey.odata", employee);
    //
    // TableColumn column1 = new TableColumn("COMPANY_ID", "Edm.Int32", true);
    // TableColumn column2 = new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", true);
    // Table model = new Table("EMPLOYEES", Arrays.asList(column1, column2), new ArrayList<>());
    // when(dbMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);
    //
    // TableColumn column7 = new TableColumn("ID", "Edm.Int32", true);
    // TableColumn column8 = new TableColumn("FK_PHONE", "Edm.Int32", false);
    // model = new Table("ADDRESS", Arrays.asList(column7, column8), new ArrayList<>());
    // when(dbMetadataUtil.getTableMetadata("ADDRESS", null)).thenReturn(model);
    //
    // TableColumn column3 = new TableColumn("NUMBER", "Edm.Int32", true);
    // TableColumn column4 = new TableColumn("FK_COMPANY_ID", "Edm.Int32", false);
    // TableColumn column5 = new TableColumn("FK_EMPLOYEE_NUMBER", "Edm.Int32", false);
    // model = new Table("PHONES", Arrays.asList(column3, column4, column5), new ArrayList<>());
    // when(dbMetadataUtil.getTableMetadata("PHONES", null)).thenReturn(model);
    //
    // String entityEmployee = "{\n" +
    // "\t\"edmType\": \"employeeType\",\n" +
    // "\t\"edmTypeFqn\": \"np.employeeType\",\n" +
    // "\t\"sqlTable\": \"EMPLOYEES\",\n" +
    // "\t\"CompanyId\": \"COMPANY_ID\",\n" +
    // "\t\"EmployeeNumber\": \"EMPLOYEE_NUMBER\",\n" +
    // "\t\"_ref_phoneType\": {\n" +
    // "\t\t\"joinColumn\" : [\n" +
    // "\t\t\t\"COMPANY_ID\",\"EMPLOYEE_NUMBER\"\n" +
    // "\t\t]\n" +
    // "\t},\n" +
    // "\t\"_pk_\" : \"COMPANY_ID,EMPLOYEE_NUMBER\"\n" +
    // "}";
    // String entityPhone = "{\n" +
    // "\t\"edmType\": \"phoneType\",\n" +
    // "\t\"edmTypeFqn\": \"np.phoneType\",\n" +
    // "\t\"sqlTable\": \"PHONES\",\n" +
    // "\t\"Number\": \"NUMBER\",\n" +
    // "\t\"FkCompanyId\": \"FK_COMPANY_ID\",\n" +
    // "\t\"FkEmployeeNumber\": \"FK_EMPLOYEE_NUMBER\",\n" +
    // "\t\"_ref_addressType\": {\n" +
    // "\t\t\"joinColumn\" : [\n" +
    // "\t\t\t\"NUMBER\"\n" +
    // "\t\t]\n" +
    // "\t},\n" +
    // "\t\"_ref_employeeType\": {\n" +
    // "\t\t\"joinColumn\" : [\n" +
    // "\t\t\t\"FK_COMPANY_ID\",\"FK_EMPLOYEE_NUMBER\"\n" +
    // "\t\t]\n" +
    // "\t},\n" +
    // "\t\"_pk_\" : \"NUMBER\"\n" +
    // "}";
    //
    // String entityAddress = "{\n" +
    // "\t\"edmType\": \"addressType\",\n" +
    // "\t\"edmTypeFqn\": \"np.addressType\",\n" +
    // "\t\"sqlTable\": \"ADDRESS\",\n" +
    // "\t\"Id\": \"ID\",\n" +
    // "\t\"FkPhone\": \"FK_PHONE\",\n" +
    // "\t\"_ref_phoneType\": {\n" +
    // "\t\t\"joinColumn\" : [\n" +
    // "\t\t\t\"FK_PHONE\"\n" +
    // "\t\t]\n" +
    // "\t},\n" +
    // "\t\"_pk_\" : \"ID\"\n" +
    // "}";
    // String[] actualResult = odata2ODataMTransformer.transform(definition);
    // assertArrayEquals(new String[]{entityEmployee, entityPhone, entityAddress}, actualResult);
    // }
    //
    // @Test
    // public void testTransformWithCompositePrimaryKeyWithoutDefinedAllEntitiesInsideOdataFile() throws
    // IOException, SQLException {
    // String employee =
    // IOUtils.toString(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeCompositePrimaryKeyWithoutEntity.odata"),
    // Charset.defaultCharset());
    // ODataDefinition definition =
    // ODataDefinitionFactory.parseOData("/transformers/EmployeeCompositePrimaryKeyWithoutEntity.odata",
    // employee);
    //
    // TableColumn column1 = new TableColumn("COMPANY_ID", "Edm.Int32", true);
    // TableColumn column2 = new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", true);
    // Table model = new Table("EMPLOYEES", Arrays.asList(column1, column2), new ArrayList<>());
    // when(dbMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);
    //
    // TableColumn column7 = new TableColumn("ID", "Edm.Int32", true);
    // TableColumn column8 = new TableColumn("FK_PHONE", "Edm.Int32", false);
    // PersistenceTableRelationModel relPhone = new PersistenceTableRelationModel("ADDRESS", "PHONES",
    // "FK_PHONE", "ID", "CONSTRAINT_8C9F7", "CONSTRAINT_INDEX_E67");
    // model = new Table("ADDRESS", Arrays.asList(column7, column8),
    // Collections.singletonList(relPhone));
    // when(dbMetadataUtil.getTableMetadata("ADDRESS", null)).thenReturn(model);
    //
    // TableColumn column3 = new TableColumn("NUMBER", "Edm.Int32", true);
    // TableColumn column4 = new TableColumn("FK_COMPANY_ID", "Edm.Int32", false);
    // TableColumn column5 = new TableColumn("FK_EMPLOYEE_NUMBER", "Edm.Int32", false);
    // PersistenceTableRelationModel rel = new PersistenceTableRelationModel("PHONES", "EMPLOYEES",
    // "FK_COMPANY_ID", "COMPANY_ID", "CONSTRAINT_8C", "CONSTRAINT_INDEX_4");
    // PersistenceTableRelationModel rel2 = new PersistenceTableRelationModel("PHONES", "EMPLOYEES",
    // "FK_EMPLOYEE_NUMBER", "EMPLOYEE_NUMBER", "CONSTRAINT_8C9", "CONSTRAINT_INDEX_43");
    // model = new Table("PHONES", Arrays.asList(column3, column4, column5), Arrays.asList(rel, rel2));
    // when(dbMetadataUtil.getTableMetadata("PHONES", null)).thenReturn(model);
    //
    // String entityEmployee = "{\n" +
    // "\t\"edmType\": \"employeeType\",\n" +
    // "\t\"edmTypeFqn\": \"np.employeeType\",\n" +
    // "\t\"sqlTable\": \"EMPLOYEES\",\n" +
    // "\t\"CompanyId\": \"COMPANY_ID\",\n" +
    // "\t\"EmployeeNumber\": \"EMPLOYEE_NUMBER\",\n" +
    // "\t\"_ref_phoneType\": {\n" +
    // "\t\t\"joinColumn\" : [\n" +
    // "\t\t\t\"COMPANY_ID\",\"EMPLOYEE_NUMBER\"\n" +
    // "\t\t]\n" +
    // "\t},\n" +
    // "\t\"_pk_\" : \"COMPANY_ID,EMPLOYEE_NUMBER\"\n" +
    // "}";
    // String entityPhone = "{\n" +
    // "\t\"edmType\": \"phoneType\",\n" +
    // "\t\"edmTypeFqn\": \"np.phoneType\",\n" +
    // "\t\"sqlTable\": \"PHONES\",\n" +
    // "\t\"Number\": \"NUMBER\",\n" +
    // "\t\"FkCompanyId\": \"FK_COMPANY_ID\",\n" +
    // "\t\"FkEmployeeNumber\": \"FK_EMPLOYEE_NUMBER\",\n" +
    // "\t\"_ref_employeeType\": {\n" +
    // "\t\t\"joinColumn\" : [\n" +
    // "\t\t\t\"FK_COMPANY_ID\",\"FK_EMPLOYEE_NUMBER\"\n" +
    // "\t\t]\n" +
    // "\t},\n" +
    // "\t\"_pk_\" : \"NUMBER\"\n" +
    // "}";
    //
    // String[] actualResult = odata2ODataMTransformer.transform(definition);
    // assertArrayEquals(new String[]{entityEmployee, entityPhone}, actualResult);
    // }
    //
    // @Test(expected = OData2TransformerException.class)
    // public void testTransformWithCompositePrimaryKeyWhenDBIsInconsistentWithOdataFile() throws
    // IOException, SQLException {
    // String employee =
    // IOUtils.toString(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeCompositePrimaryKey.odata"),
    // Charset.defaultCharset());
    // ODataDefinition definition =
    // ODataDefinitionFactory.parseOData("/transformers/EmployeeCompositePrimaryKey.odata", employee);
    //
    // when(dbMetadataUtil.getTableMetadata("PHONES", null)).thenReturn(new Table("PHONES", new
    // ArrayList<>(), new ArrayList<>()));
    // when(dbMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(new Table("EMPLOYEES", new
    // ArrayList<>(), new ArrayList<>()));
    // TableColumn column7 = new TableColumn("ID", "Edm.Int32", true);
    // TableColumn column8 = new TableColumn("FK_PHONE", "Edm.Int32", false);
    // PersistenceTableRelationModel relPhone = new PersistenceTableRelationModel("ADDRESS", "PHONES",
    // "FK_PHONE_WRONG", "ID", "CONSTRAINT_8C9F7", "CONSTRAINT_INDEX_E67");
    // Table model = new Table("ADDRESS", Arrays.asList(column7, column8),
    // Collections.singletonList(relPhone));
    // when(dbMetadataUtil.getTableMetadata("ADDRESS", null)).thenReturn(model);
    //
    // odata2ODataMTransformer.transform(definition);
    // }
    //
    // @Test
    // public void testTransformWithCompositePrimaryKeyAndAllValidProperties() throws IOException,
    // SQLException {
    // String employee =
    // IOUtils.toString(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeWithProp.odata"),
    // Charset.defaultCharset());
    // ODataDefinition definition =
    // ODataDefinitionFactory.parseOData("/transformers/EmployeeWithProp.odata", employee);
    //
    // TableColumn column1 = new TableColumn("COMPANY_ID", "Edm.Int32", true);
    // TableColumn column2 = new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", true);
    // TableColumn column3 = new TableColumn("ORDER_ID", "Edm.Int32", false);
    // Table model = new Table("EMPLOYEES", Arrays.asList(column1, column2, column3), new
    // ArrayList<>());
    // when(dbMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);
    //
    // TableColumn column5 = new TableColumn("NUMBER", "Edm.Int32", true);
    // TableColumn column6 = new TableColumn("FK_COMPANY_ID", "Edm.Int32", false);
    // TableColumn column7 = new TableColumn("FK_EMPLOYEE_NUMBER", "Edm.Int32", false);
    // model = new Table("PHONES", Arrays.asList(column5, column6, column7), new ArrayList<>());
    // when(dbMetadataUtil.getTableMetadata("PHONES", null)).thenReturn(model);
    //
    // String entityEmployee = "{\n" +
    // "\t\"edmType\": \"employeeType\",\n" +
    // "\t\"edmTypeFqn\": \"np.employeeType\",\n" +
    // "\t\"sqlTable\": \"EMPLOYEES\",\n" +
    // "\t\"companyId\": \"COMPANY_ID\",\n" +
    // "\t\"employeeNumber\": \"EMPLOYEE_NUMBER\",\n" +
    // "\t\"orderId\": \"ORDER_ID\",\n" +
    // "\t\"_pk_\" : \"COMPANY_ID,EMPLOYEE_NUMBER\"\n" +
    // "}";
    // String phoneEntity = "{\n" +
    // "\t\"edmType\": \"phoneType\",\n" +
    // "\t\"edmTypeFqn\": \"np.phoneType\",\n" +
    // "\t\"sqlTable\": \"PHONES\",\n" +
    // "\t\"Number\": \"NUMBER\",\n" +
    // "\t\"FkCompanyId\": \"FK_COMPANY_ID\",\n" +
    // "\t\"FkEmployeeNumber\": \"FK_EMPLOYEE_NUMBER\",\n" +
    // "\t\"_pk_\" : \"NUMBER\"\n" +
    // "}";
    // String[] actualResult = odata2ODataMTransformer.transform(definition);
    // assertArrayEquals(new String[]{entityEmployee, phoneEntity}, actualResult);
    // }
    //
    // @Test
    // public void testTransformWithCompositePrimaryKeyWithLessDbPropsExposed() throws IOException,
    // SQLException {
    // String employee =
    // IOUtils.toString(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeWithProp.odata"),
    // Charset.defaultCharset());
    // ODataDefinition definition =
    // ODataDefinitionFactory.parseOData("/transformers/EmployeeWithProp.odata", employee);
    //
    // TableColumn column1 = new TableColumn("COMPANY_ID", "Edm.Int32", true);
    // TableColumn column2 = new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", true);
    // TableColumn column3 = new TableColumn("ORDER_ID", "Edm.Int32", false);
    // TableColumn column4 = new TableColumn("ADDRESS_ID", "Edm.Int32", false);
    // Table model = new Table("EMPLOYEES", Arrays.asList(column1, column2, column3, column4), new
    // ArrayList<>());
    // when(dbMetadataUtil.getTableMetadata("EMPLOYEES",null)).thenReturn(model);
    //
    // TableColumn column5 = new TableColumn("NUMBER", "Edm.Int32", true);
    // TableColumn column6 = new TableColumn("FK_COMPANY_ID", "Edm.Int32", false);
    // TableColumn column7 = new TableColumn("FK_EMPLOYEE_NUMBER", "Edm.Int32", false);
    // model = new Table("PHONES", Arrays.asList(column5, column6, column7), new ArrayList<>());
    // when(dbMetadataUtil.getTableMetadata("PHONES", null)).thenReturn(model);
    //
    // String entityEmployee = "{\n" +
    // "\t\"edmType\": \"employeeType\",\n" +
    // "\t\"edmTypeFqn\": \"np.employeeType\",\n" +
    // "\t\"sqlTable\": \"EMPLOYEES\",\n" +
    // "\t\"companyId\": \"COMPANY_ID\",\n" +
    // "\t\"employeeNumber\": \"EMPLOYEE_NUMBER\",\n" +
    // "\t\"orderId\": \"ORDER_ID\",\n" +
    // "\t\"_pk_\" : \"COMPANY_ID,EMPLOYEE_NUMBER\"\n" +
    // "}";
    // String phoneEntity = "{\n" +
    // "\t\"edmType\": \"phoneType\",\n" +
    // "\t\"edmTypeFqn\": \"np.phoneType\",\n" +
    // "\t\"sqlTable\": \"PHONES\",\n" +
    // "\t\"Number\": \"NUMBER\",\n" +
    // "\t\"FkCompanyId\": \"FK_COMPANY_ID\",\n" +
    // "\t\"FkEmployeeNumber\": \"FK_EMPLOYEE_NUMBER\",\n" +
    // "\t\"_pk_\" : \"NUMBER\"\n" +
    // "}";
    //
    // String[] actualResult = odata2ODataMTransformer.transform(definition);
    // assertArrayEquals(new String[]{entityEmployee, phoneEntity}, actualResult);
    // }
    //
    // @Test(expected = OData2TransformerException.class)
    // public void testTransformWithCompositePrimaryKeyAndLessNumberOfDBProps() throws IOException,
    // SQLException {
    // String employee =
    // IOUtils.toString(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeWithProp.odata"),
    // Charset.defaultCharset());
    // ODataDefinition definition =
    // ODataDefinitionFactory.parseOData("/transformers/EmployeeWithProp.odata", employee);
    //
    // TableColumn column1 = new TableColumn("COMPANY_ID", "Edm.Int32", true);
    // TableColumn column2 = new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", true);
    // Table model = new Table("EMPLOYEES", Arrays.asList(column1, column2), new ArrayList<>());
    // when(dbMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);
    //
    // odata2ODataMTransformer.transform(definition);
    // }
    //
    // @Test(expected = OData2TransformerException.class)
    // public void testTransformWithCompositePrimaryKeyAndWrongAssProps() throws IOException,
    // SQLException {
    // String employee =
    // IOUtils.toString(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeWithWrongAssProps.odata"),
    // Charset.defaultCharset());
    // ODataDefinition definition =
    // ODataDefinitionFactory.parseOData("/transformers/EmployeeWithWrongAssProps.odata", employee);
    //
    // TableColumn column1 = new TableColumn("COMPANY_ID", "Edm.Int32", true);
    // TableColumn column2 = new TableColumn("EMPLOYEE_NUMBER", "Edm.Int32", true);
    // Table model = new Table("EMPLOYEES", Arrays.asList(column1, column2), new ArrayList<>());
    // when(dbMetadataUtil.getTableMetadata("EMPLOYEES", null)).thenReturn(model);
    //
    // odata2ODataMTransformer.transform(definition);
    // }

}
