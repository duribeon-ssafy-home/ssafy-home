# AGENTS.md

## Project Overview

This is a Vue 3 frontend project using:

- Vue Router
- Pinia
- Axios
- SCSS
- ESLint
- Prettier

Use Vue 3 Composition API with `<script setup>`.

This project must use JavaScript only. Do not introduce TypeScript unless explicitly requested.

## Commands

- Install dependencies: `pnpm install`
- Run dev server: `pnpm dev`
- Build: `pnpm build`
- Lint: `pnpm lint` if configured
- Format: `pnpm format` if configured

## Dependency Rules

- If a required dependency is missing, install it with `pnpm add` or `pnpm add -D`.
- Use `pnpm` as the package manager.
- Do not use `npm`, `yarn`, or `bun` unless explicitly requested.
- Do not add new libraries without explaining why.
- Before adding a new library, check whether the project already has an equivalent dependency.

## Coding Rules

- Use Vue 3 `<script setup>`.
- Use JavaScript only.
- Do not create `.ts` files.
- Do not use `<script setup lang="ts">`.
- Use Pinia for shared state.
- Use Vue Router for routing.
- Use SCSS with `<style lang="scss" scoped>`.
- Do not modify unrelated files.

## Router Rules

- Define routes in `src/router/index.js`.
- Use named routes.
- Use `meta.requiresAuth` for protected pages.
- Redirect unauthenticated users to login with a `redirect` query.

## Pinia Rules

- Put stores in `src/stores`.
- Use setup-style stores.
- Keep authentication state in `useAuthStore`.
- Use store actions instead of changing shared state directly in components.

## Axios Rules

- Create a shared Axios instance in `src/api/axios.js`.
- Do not import raw `axios` directly in Vue components.
- Put API functions in `src/api/*Api.js`.
- Use request interceptors to attach the access token.
- Keep response interceptor logic simple unless refresh token handling is implemented.

## API Response Rules

- When implementing frontend API integration, follow the actual backend response format.
- If backend code is available in the repository, check related controllers, DTOs, and response objects before assuming response fields.
- Do not invent response fields.
- Check existing API functions before creating new request/response handling logic.
- Keep API response handling consistent across the project.
- Do not modify backend code unless the task explicitly requires it.

## SCSS Rules

- Use component-scoped SCSS by default.
- Keep nesting shallow, preferably 2 levels or less.
- Put shared variables in `src/assets/styles/_variables.scss`.
- Put global styles in `src/assets/styles/main.scss`.

## Project Structure Rules

- Page-level components should be placed in `src/views`.
- Reusable components should be placed in `src/components`.
- API request functions should be placed in `src/api`.
- Pinia stores should be placed in `src/stores`.
- Router configuration should be placed in `src/router/index.js`.
- Shared SCSS files should be placed in `src/assets/styles`.

## Component Rules

- Use PascalCase for component file names.
- Use `View` suffix for page components, such as `LoginView.vue`.
- Keep business logic out of templates when possible.
- Use computed properties for derived values.
- Do not use `v-if` and `v-for` on the same element.
- Always provide a unique `:key` when using `v-for`.

## Authentication Rules

- Store login state in `useAuthStore`.
- Use `accessToken` as the main authentication token.
- Store the access token in `localStorage` unless a different auth policy is introduced.
- Attach the access token through the Axios request interceptor.
- Do not check authentication separately in every component.
- Use router guards for page-level authentication checks.
- After login, redirect to the `redirect` query if it exists.

## Environment Rules

- Use `VITE_API_BASE_URL` for the backend API base URL.
- Do not hardcode backend URLs directly in components.
- Environment variables should be accessed through `import.meta.env`.

## Error Handling Rules

- Use `try/catch` around API calls in stores or view-level components.
- Do not silently ignore errors.
- Show user-facing error messages when API requests fail.
- Keep low-level Axios error handling simple unless refresh token handling is implemented.