var JavaPackages = new JavaImporter(
	Packages.ray.rml.Vector3f,
	Packages.java.awt.Color
	);

with (JavaPackages) {
	var ambientColor = new Color(.0, .0, .0);
	var diffuseColor = new Color(.75, .75, .75);
	var specularColor = new Color(.75, .7, 1.0);
	var lightDirection = Vector3f.createFrom(-1.8, 1, 1);
	var spawnPoint = Vector3f.createFrom(0, 2, -30);
	var puckSpawnPoint = Vector3f.createFrom(0, 5, -10);
}