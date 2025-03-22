# MPP Project - Java Implementation

## Overview
This repository contains the Java implementation of the "Medii de proiectare și programare" (Design and Programming Environments) course project. The project follows a layered architecture with Model, Repository, Service, and UI layers.

## Project Structure

The project is organized by weekly assignments with each assignment building upon the previous one:

### Week 1: Models and IRepository
- Implemented domain model classes
- Created repository interfaces (IRepository)
- Set up basic project structure

### Week 2: Repository Implementation
- Implemented interfaces for repository
- Created database repositories with JDBC
- Set up properties file for database connection and configuration
- Added logging for repository operations
- Connected to relational database

### Week 3: Services and UI
- Implemented service layer with business logic
- Created graphical user interface using JavaFX
- Connected UI controllers to service methods
- Service layer acts as an intermediary between UI and repositories

## Branching Strategy
The project uses different branches for each weekly assignment:

- `lab1`: Models and IRepository implementation
- `lab2`: Repository implementation and database connection
- `lab3`: Services and UI implementation
- `main`: Latest stable version
