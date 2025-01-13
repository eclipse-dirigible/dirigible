declare module "sdk/test" {
    
    module assert {
        /**
         * Assert the True Condition
         * @param condition
         * @param message
         */
        function assertTrue(condition: boolean, message: string);
        
        /**
         * Assert the False Condition
         * @param condition
         * @param message
         */
        function assertFalse(condition: boolean, message: string);
        
        /**
         * Assert the Null Object
         * @param object
         * @param message
         */
        function assertNull(object: any, message: string);
        
        /**
         * Assert the Not Null Object
         * @param object
         * @param message
         */
        function assertNotNull(object: any, message: string);
        
         /**
         * Assert the Equals
         * @param actual
         * @param expected
         * @param message
         */
        function assertEquals(actual: any, expected: any, message: string);
        
    }
    
    module runner {
        function run(settings);
    }
    
}
