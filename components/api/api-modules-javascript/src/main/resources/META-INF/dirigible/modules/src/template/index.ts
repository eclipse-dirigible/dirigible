/**
 * @module template/index
 * @package @aerokit/sdk/template
 * @overview
 *
 * This module provides template engine utilities for the Aerokit SDK.
 * It exposes various template engines for rendering dynamic content,
 * supporting multiple template languages and formatting options.
 *
 * The main components of this module are:
 * - TemplateEngines: Registry and management of available template engines.
 */

export { TemplateEngines as engines } from "./engines";
export * from "./engines";