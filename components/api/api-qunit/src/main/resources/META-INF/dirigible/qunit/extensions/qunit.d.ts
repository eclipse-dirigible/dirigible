declare module "sdk/qunit" {
    module qunit {
        /**
         * Register a module by name
         * @param name the name of the module
         **/
        function module(name: string): string

        /**
         * Register a group of tests
         * @param name the name of the module
         * @param group group of tests
         **/
        function test(name: string, group): string
    }
    module runner {
        /**
         * Run the tests
         **/
        function run()
    }
}
