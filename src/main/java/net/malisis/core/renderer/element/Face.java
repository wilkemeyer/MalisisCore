/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Ordinastie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.malisis.core.renderer.element;

import java.util.HashMap;
import java.util.List;

import net.malisis.core.renderer.RenderParameters;
import net.malisis.core.renderer.animation.transformation.ITransformable;
import net.malisis.core.renderer.icon.MalisisIcon;
import net.malisis.core.util.Vector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;

import com.google.common.primitives.Ints;

public class Face implements ITransformable.Translate, ITransformable.Rotate
{
	protected String name;
	protected Vertex[] vertexes;
	protected RenderParameters params;

	public Face(Vertex[] vertexes, RenderParameters params)
	{
		this.vertexes = vertexes;
		this.params = params != null ? params : new RenderParameters();
		this.setName(null);
	}

	public Face(Vertex... vertexes)
	{
		this(vertexes, null);
	}

	public Face(List<Vertex> vertexes)
	{
		this(vertexes.toArray(new Vertex[0]), null);
	}

	public Face(Face face)
	{
		this(face, new RenderParameters(face.params));
	}

	public Face(Face face, RenderParameters params)
	{
		Vertex[] faceVertexes = face.getVertexes();
		this.vertexes = new Vertex[faceVertexes.length];
		for (int i = 0; i < faceVertexes.length; i++)
			vertexes[i] = new Vertex(faceVertexes[i]);
		this.params = params != null ? params : new RenderParameters();
		name = face.name;
	}

	/**
	 * Sets the base name for this {@link Face}. If the name specified is null, it is automatically determined based on the {@link Vertex}
	 * positions.
	 *
	 * @param name the base name
	 */
	public void setName(String name)
	{
		if (name == null)
		{
			name = "";
			HashMap<String, Integer> map = new HashMap<String, Integer>();
			String[] dirs = new String[] { "North", "South", "East", "West", "Top", "Bottom" };
			for (String dir : dirs)
			{
				map.put(dir, 0);
				for (Vertex v : vertexes)
				{
					if (v.name().contains(dir))
						map.put(dir, map.get(dir) + 1);
				}
				if (map.get(dir) == 4)
					name = dir;
			}
		}

		this.name = name;
	}

	/**
	 * Gets the base name of this {@link Face}.
	 *
	 * @return the base name
	 */
	public String name()
	{
		return name;
	}

	/**
	 * Gets the {@link Vertex vertexes} of this {@link Face}.
	 *
	 * @return the vertexes
	 */
	public Vertex[] getVertexes()
	{
		return vertexes;
	}

	/**
	 * Sets the {@link RenderParameters} for this {@link Face}.
	 *
	 * @param params the parameters. If {@code null}, sets default parameters
	 * @return this {@link Face}
	 */
	public Face setParameters(RenderParameters params)
	{
		this.params = params != null ? params : new RenderParameters();
		return this;
	}

	/**
	 * Gets the {@link RenderParameters} of this {@link Face}.
	 *
	 * @return the parameters
	 */
	public RenderParameters getParameters()
	{
		return params;
	}

	public Face setColor(int color)
	{
		for (Vertex v : vertexes)
			v.setColor(color);
		return this;
	}

	public Face setAlpha(int alpha)
	{
		for (Vertex v : vertexes)
			v.setAlpha(alpha);
		return this;
	}

	public Face setBrightness(int brightness)
	{
		for (Vertex v : vertexes)
			v.setBrightness(brightness);
		return this;
	}

	//#region Textures manipaluation
	public Face setTexture(MalisisIcon icon)
	{
		return setTexture(icon, params.flipU.get(), params.flipV.get(), false);
	}

	public Face setStandardUV()
	{
		vertexes[0].setUV(0, 0);
		vertexes[1].setUV(0, 1);
		vertexes[2].setUV(1, 1);
		vertexes[3].setUV(1, 0);
		return this;
	}

	public Face interpolateUV()
	{
		float u = 0;
		float v = 0;
		float U = 1;
		float V = 1;

		double factorU, factorV;

		float uvs[][] = new float[vertexes.length][2];
		for (int i = 0; i < vertexes.length; i++)
		{
			Vertex vertex = vertexes[i];

			factorU = getFactorU(vertex);
			factorV = getFactorV(vertex);

			int k = i;
			uvs[k] = new float[] { interpolate(u, U, factorU, false), interpolate(v, V, factorV, false) };
		}

		for (int i = 0; i < vertexes.length; i++)
			vertexes[i].setUV(uvs[i][0], uvs[i][1]);

		return this;
	}

	public Face setTexture(MalisisIcon icon, boolean flippedU, boolean flippedV, boolean interpolate)
	{
		if (icon == null)
			return this;

		float u = icon.getMinU();
		float v = icon.getMinV();
		float U = icon.getMaxU();
		float V = icon.getMaxV();

		double factorU, factorV;

		float uvs[][] = new float[vertexes.length][2];
		for (int i = 0; i < vertexes.length; i++)
		{
			Vertex vertex = vertexes[i];

			factorU = interpolate ? getFactorU(vertex) : vertex.getU();
			factorV = interpolate ? getFactorV(vertex) : vertex.getV();

			int k = i;
			if (icon instanceof MalisisIcon)
			{
				k = (i + icon.getRotation()) % vertexes.length;
			}
			uvs[k] = new float[] { interpolate(u, U, factorU, flippedU), interpolate(v, V, factorV, flippedV) };
		}

		for (int i = 0; i < vertexes.length; i++)
			vertexes[i].setUV(uvs[i][0], uvs[i][1]);

		return this;
	}

	private double getFactorU(Vertex vertex)
	{
		if (params.direction.get() == null)
			return vertex.getU();

		switch (params.direction.get())
		{
			case EAST:
				return vertex.getZ();
			case WEST:
				return vertex.getZ();
			case NORTH:
				return vertex.getX();
			case SOUTH:
			case UP:
			case DOWN:
				return vertex.getX();
			default:
				return 0;
		}
	}

	private double getFactorV(Vertex vertex)
	{
		if (params.direction.get() == null)
			return vertex.getV();

		switch (params.direction.get())
		{
			case EAST:
			case WEST:
			case NORTH:
			case SOUTH:
				return 1 - vertex.getY();
			case UP:
			case DOWN:
				return vertex.getZ();
			default:
				return 0;
		}
	}

	private float interpolate(float min, float max, double factor, boolean flipped)
	{
		if (factor > 1)
			factor = 1;
		if (factor < 0)
			factor = 0;
		if (flipped)
			factor = 1 - factor;

		return min + (max - min) * (float) factor;
	}

	//#end Textures manipaluation

	//#region Transformations
	@Override
	public void translate(float x, float y, float z)
	{
		for (Vertex v : vertexes)
			v.translate(x, y, z);
	}

	public void scale(float f)
	{
		scale(f, f, f, 0, 0, 0);
	}

	public void scale(float f, float offset)
	{
		scale(f, f, f, offset, offset, offset);
	}

	public void scale(float fx, float fy, float fz)
	{
		scale(fx, fy, fz, 0, 0, 0);
	}

	public void scale(float fx, float fy, float fz, float offsetX, float offsetY, float offsetZ)
	{
		for (Vertex v : vertexes)
			v.scale(fx, fy, fz, offsetX, offsetY, offsetZ);
	}

	@Override
	public void rotate(float angle, float x, float y, float z, float offsetX, float offsetY, float offsetZ)
	{
		rotateAroundX(angle * x, offsetX, offsetY, offsetZ);
		rotateAroundY(angle * y, offsetX, offsetY, offsetZ);
		rotateAroundZ(angle * z, offsetX, offsetY, offsetZ);
	}

	public void rotateAroundX(double angle)
	{
		rotateAroundX(angle, 0.5, 0.5, 0.5);
	}

	public void rotateAroundX(double angle, double centerX, double centerY, double centerZ)
	{
		for (Vertex v : vertexes)
			v.rotateAroundX(angle, centerX, centerY, centerZ);
	}

	public void rotateAroundY(double angle)
	{
		rotateAroundY(angle, 0.5, 0.5, 0.5);
	}

	public void rotateAroundY(double angle, double centerX, double centerY, double centerZ)
	{
		for (Vertex v : vertexes)
			v.rotateAroundY(angle, centerX, centerY, centerZ);
	}

	public void rotateAroundZ(double angle)
	{
		rotateAroundZ(angle, 0.5, 0.5, 0.5);
	}

	public void rotateAroundZ(double angle, double centerX, double centerY, double centerZ)
	{
		for (Vertex v : vertexes)
			v.rotateAroundZ(angle, centerX, centerY, centerZ);
	}

	//#end Transformations

	/**
	 * Automatically calculate AoMatrix for this {@link Face}. Only works for regular N/S/E/W/T/B faces
	 *
	 * @param offset the offset
	 * @return the aoMatrix
	 */
	public int[][][] calculateAoMatrix(EnumFacing offset)
	{
		int[][][] aoMatrix = new int[vertexes.length][3][3];

		for (int i = 0; i < vertexes.length; i++)
			aoMatrix[i] = vertexes[i].getAoMatrix(offset);

		return aoMatrix;
	}

	/**
	 * Gets the vertexes normals for this {@link Face}.
	 *
	 * @return the vertexes normals
	 */
	public Vector[] getVertexNormals()
	{
		Vector[] normals = new Vector[vertexes.length];
		int i = 0;
		for (Vertex v : vertexes)
			normals[i++] = new Vector(v.getX(), v.getY(), v.getZ());
		return normals;
	}

	/**
	 * Calculates the normal of this {@link Face} based on the vertex coordinates.
	 */
	public void calculateNormal()
	{
		calculateNormal(getVertexNormals());
	}

	/**
	 * Calculates normal of this {@link Face} using the vertex normals provided.
	 *
	 * @param normals the normals
	 * @return the vector
	 */
	public Vector calculateNormal(Vector[] normals)
	{
		if (normals == null || normals.length != vertexes.length)
			normals = getVertexNormals();

		double x = 0;
		double y = 0;
		double z = 0;

		for (int i = 0; i < vertexes.length; i++)
		{
			Vertex current = vertexes[i];
			Vertex next = vertexes[(i + 1) % vertexes.length];

			x += (current.getY() - next.getY()) * (current.getZ() + next.getZ());
			y += (current.getZ() - next.getZ()) * (current.getX() + next.getX());
			z += (current.getX() - next.getX()) * (current.getY() + next.getY());
		}

		int factor = 1000;
		Vector normal = new Vector((float) Math.round(x * factor) / factor, (float) Math.round(y * factor) / factor, (float) Math.round(z
				* factor)
				/ factor);
		normal.normalize();
		return normal;
	}

	/**
	 * Deducts the parameters for this {@link Face} based on the calculated normal.
	 */
	public void deductParameters()
	{
		deductParameters(getVertexNormals());
	}

	/**
	 * Deducts the {@link RenderParameters} for this {@link Face} based on the specified normals
	 *
	 * @param normals the vertex normals
	 */
	public void deductParameters(Vector[] normals)
	{
		Vector normal = calculateNormal(normals);
		EnumFacing dir = null;

		if (normal.x == 0 && normal.y == 0)
		{
			if (normal.z == 1)
				dir = EnumFacing.SOUTH;
			else if (normal.z == -1)
				dir = EnumFacing.NORTH;
		}
		else if (normal.x == 0 && normal.z == 0)
		{
			if (normal.y == 1)
				dir = EnumFacing.UP;
			else if (normal.y == -1)
				dir = EnumFacing.DOWN;
		}
		else if (normal.y == 0 && normal.z == 0)
		{
			if (normal.x == 1)
				dir = EnumFacing.EAST;
			else if (normal.x == -1)
				dir = EnumFacing.WEST;
		}

		params.direction.set(dir);
		params.textureSide.set(dir);
		params.aoMatrix.set(calculateAoMatrix(dir));

		//fry's patent
		float f = (float) ((normal.x * normal.x * 0.6 + normal.y * (normal.y * 3 + 1) / 4 + normal.z * normal.z * 0.8));
		params.colorFactor.set(f);
	}

	/**
	 * Gets a {@link BakedQuad} from this {@link Face}
	 *
	 * @return the baked quad
	 */
	public BakedQuad toBakedQuad()
	{
		int[] data = new int[0];
		for (Vertex v : getVertexes())
			data = Ints.concat(data, v.toVertexData());

		return new BakedQuad(data, params.colorMultiplier.get(), params.direction.get());
	}

	@Override
	public String toString()
	{
		String s = name() + " {";
		for (Vertex v : vertexes)
			s += v.name() + ", ";
		return s + "}";
	}

	/**
	 * Gets a {@link Face} name from a {@link EnumFacing}.
	 *
	 * @param dir the dir
	 * @return the name
	 */
	public static String nameFromDirection(EnumFacing dir)
	{
		if (dir == EnumFacing.UP)
			return "top";
		else if (dir == EnumFacing.DOWN)
			return "bottom";
		else
			return dir.toString();
	}

}
