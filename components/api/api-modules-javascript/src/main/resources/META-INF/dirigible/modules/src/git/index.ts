/**
 * @module git/index
 * @package @aerokit/sdk/git
 * @overview
 * 
 * This module provides functionalities for interacting with Git repositories within the Aerokit SDK. It includes classes and methods for connecting to Git repositories, performing operations such as cloning, committing, pushing, and pulling changes, and managing Git configurations.
 * 
 * The main components of this module are:
 * - Client: Represents a client for connecting to a Git repository and provides methods to perform various Git operations.
 */

export * from "./client";
export { Client as client } from "./client";
