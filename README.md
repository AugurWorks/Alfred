# Alfred
Alfred is the AugurWorks butler, happily providing tomorrow's stock prices today.

## Docker
### Docker Install
Install [Docker Toolbox](https://www.docker.com/products/docker-toolbox) for Docker on Windows and Mac. Docker can be natively installed on Linux. Detailed install instructions for Linux can be found on the [Docker install page](https://docs.docker.com/engine/installation/).

### Run Without Building
To run **Alfred** locally without building it follow the configuration step at the top of the **Tag and Push** section then call the final command in that section to pull and run the container locally.

### Build
To build the WAR file within Docker run the following:

```bash
docker build -t alfred .
```

### Run
To run the app after building run the following:

```bash
docker rm -f alfred # Remove existing alfred container
docker run -d --name=alfred -p 8080:8080 alfred
```

To confirm the project is running got to [http://[docker-ip]:8080/](http://[docker-ip]:8080/) and confirm you are redirected to the Swagger UI page.

#### Volumes
By default Docker containers will not persist nets between Docker runs. There is a volume location available for persisting nets between runs. Add the volume parameter (`-v /local/path/location:/usr/local/tomcat/nets`) to persist the nets to the host machine.

#### Environment Variables
The Docker container can be run with certain environment variables to customize the container. These can be passed with the `-e VARIABLE=value` flag on the `docker run` command. Below are the variables and the defaults:

- **RABBITMQ_USERNAME** (default: guest) - RabbitMQ username
- **RABBITMQ_PASSWORD** (default: guest) - RabbitMQ password
- **RABBITMQ_HOST** (default: rabbitmq) - RabbitMQ hostname
- **RABBITMQ_PORT** (default: 5672) - RabbitMQ port

## Tag and Push
**NOTE:** Info on configuring the AWS command line for ECR and the repo referred to here can be found [here](https://console.aws.amazon.com/ecs/home?region=us-east-1#/repositories/alfred#images)

After building tag the local build then push the current version and change the latest tag with the following:

```bash
# Log in to AWS
eval `aws ecr get-login [--profile aws-profile-name]`

bash docker-tag.sh [version]
```

To run the remote container run the following after logging into the AWS ECR:

```bash
docker run -d --name=alfred -p 80:8080 -e FLUENTD_HOST=docker.augurworks.com -e HOSTNAME=alfred --volumes-from data 274685854631.dkr.ecr.us-east-1.amazonaws.com/alfred:[version]
```
