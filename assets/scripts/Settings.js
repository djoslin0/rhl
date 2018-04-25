var JavaPackages = new JavaImporter(
	Packages.ray.rml.Vector3f,
	Packages.java.awt.Color
	);

with (JavaPackages) {
	var ambientColor = new Color(.3, .3, .3);
	var diffuseColor = new Color(.75, .75, .75);
	var specularColor = new Color(.75, .7, 1.0);
	var lightDirection = Vector3f.createFrom(-1.8, 1, -1);
	
	var spawnPoint = Vector3f.createFrom(0, 1.8, -30);
	var puckSpawnPoint = Vector3f.createFrom(0, 5, -10);

	var terrainEmissive = new Color(.2, .2, .2);
	var terrainSpecular = new Color(.5, .5, .5);

	var goalDistance = 70.0;

	var debug1 = 0.01;
	var debug2 = 0.0;
	var debug3 = 0.0;
	var debug4 = 0.0;
	var debug5 = 0.0;

}