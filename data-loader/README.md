# Data Loader
Data Loader submits previously run data sets for local testing.

## Requirements
Data Loader requires Groovy to be installed locally. It also requires the RabbitMQ portion of [AW Docker](https://github.com/AugurWorks/aw-docker).

## Usage
Place dataset JSON files in the `data/` folder before using. Example usage is shown below:
```bash
groovy data-loader.groovy 17-10-alfred-da5aba4e-f86c-4951-8258-fbed92623e6b.json
```