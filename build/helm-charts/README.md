# Eclipse Dirigible

[Eclipse Dirigible](https://www.dirigible.io/) is Cloud Development platform, providing full-stack Web Application Development tools and runtime.
Some of the key features are:
- In-System development
- Low-code/No-code development
- Modeling and Generation from templates
- In-app extensibility
- Enterprise JavaScript APIs
- Web IDE

Add Dirigible Helm repo:

```console
$ helm repo add dirigible https://eclipse-dirigible.github.io/dirigible
```

Update repo:

```console
$ helm repo update
```

Install Dirigible with Helm:

```console
$ helm install dirigible dirigible/dirigible
```

Resources:
- [dirigible.io](https://www.dirigible.io)
- [github.com/eclipse/dirigible](https://github.com/eclipse/dirigible)
- [youtube.com/c/dirigibleio](https://www.youtube.com/c/dirigibleio)


## Manual Helm Charts Update:

1. Navigate to the `helm-chart` folder:
    ```
    cd releng/helm-charts/
    ```
1. Set the Dirigible version in `dirigible/Chart.yaml`:

    > Replace the `#{DirigibleVersion}#` placeholder.

1. Package Helm Chart:

    ```
    helm package dirigible
    ```

1. Copy the `dirigible-5.8.4.tgz` somewhere outside the Git repository.

1. Reset all changes:

    ```
    git add .
    git reset --hard
    cd ../../
    ```

1. Switch to the `gh-pages` branch:

    ```
    git checkout gh-pages
    git pull origin gh-pages
    ```

1. Paste the `dirigible-5.8.4.tgz` chart into the `charts` directory.

1. Build Helm Index:

    ```
    helm repo index charts/ --url https://eclipse-dirigible.github.io/dirigible/charts
    ```

1. Move the `charts/index.yaml` to the root folder:

    ```
    mv charts/index.yaml .
    ```

1. Push the changes:

    ```
    git add index.yaml
    git add charts/

    git commit -m "Helm Charts Updated"

    git push origin gh-pages
    ```
