// Made with Blockbench 4.12.3
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class ModelSubstitution<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "substitution"), "main");
	private final ModelPart block;

	public ModelSubstitution(ModelPart root) {
		this.block = root.getChild("block");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition block = partdefinition.addOrReplaceChild("block", CubeListBuilder.create().texOffs(0, 0).addBox(
				-4.0F, -8.0F, -5.0F, 9.0F, 18.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 14.0F, 0.0F));

		PartDefinition block_r1 = block.addOrReplaceChild("block_r1",
				CubeListBuilder.create().texOffs(16, 27).addBox(5.0216F, -15.4016F, 0.1277F, 3.0F, 10.0F, 3.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-3.2716F, -0.5984F, -4.4277F, -0.5472F, -0.2564F, 0.6684F));

		PartDefinition block_r2 = block.addOrReplaceChild("block_r2",
				CubeListBuilder.create().texOffs(0, 27).addBox(-4.3438F, -2.7188F, -4.9063F, 4.0F, 11.0F, 4.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-4.2716F, -0.5984F, -5.1777F, 0.5639F, -0.0308F, -0.5905F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		block.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
	}
}