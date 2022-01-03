/*
 * Copyright 2018-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.type.classreading;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.data.type.MethodsMetadata;
import org.springframework.lang.Nullable;

/**
 * Extension of {@link SimpleMetadataReaderFactory} that reads {@link MethodsMetadata}, creating a new ASM
 * {@link MethodsMetadataReader} for every request.
 *
 * @author Mark Paluch
 * @since 2.1
 * @deprecated since 3.0. Use {@link SimpleMetadataReaderFactory} directly.
 */
@Deprecated
public class MethodsMetadataReaderFactory extends SimpleMetadataReaderFactory {

	/**
	 * Create a new {@link MethodsMetadataReaderFactory} for the default class loader.
	 */
	public MethodsMetadataReaderFactory() {}

	/**
	 * Create a new {@link MethodsMetadataReaderFactory} for the given {@link ResourceLoader}.
	 *
	 * @param resourceLoader the Spring {@link ResourceLoader} to use (also determines the {@link ClassLoader} to use).
	 */
	public MethodsMetadataReaderFactory(@Nullable ResourceLoader resourceLoader) {
		super(resourceLoader);
	}

	/**
	 * Create a new {@link MethodsMetadataReaderFactory} for the given {@link ClassLoader}.
	 *
	 * @param classLoader the class loader to use.
	 */
	public MethodsMetadataReaderFactory(@Nullable ClassLoader classLoader) {
		super(classLoader);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.type.classreading.SimpleMetadataReaderFactory#getMetadataReader(java.lang.String)
	 */
	@Override
	public MethodsMetadataReader getMetadataReader(String className) throws IOException {
		return new MetadataReaderWrapper(super.getMetadataReader(className));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.type.classreading.SimpleMetadataReaderFactory#getMetadataReader(org.springframework.core.io.Resource)
	 */
	@Override
	public MethodsMetadataReader getMetadataReader(Resource resource) throws IOException {
		return new MetadataReaderWrapper(super.getMetadataReader(resource));
	}

	private static class MetadataReaderWrapper implements MethodsMetadataReader {

		private final MetadataReader delegate;

		MetadataReaderWrapper(MetadataReader delegate) {
			this.delegate = delegate;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.type.classreading.MethodsMetadataReader#getMethodsMetadata()
		 */
		@Override
		public MethodsMetadata getMethodsMetadata() {
			return new MethodsMetadataWrapper(getAnnotationMetadata(), getClassMetadata());
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.classreading.MetadataReader#getResource()
		 */
		@Override
		public Resource getResource() {
			return delegate.getResource();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.classreading.MetadataReader#getClassMetadata()
		 */
		@Override
		public ClassMetadata getClassMetadata() {
			return delegate.getClassMetadata();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.classreading.MetadataReader#getAnnotationMetadata()
		 */
		@Override
		public AnnotationMetadata getAnnotationMetadata() {
			return delegate.getAnnotationMetadata();
		}

	}

	private static class MethodsMetadataWrapper implements MethodsMetadata, ClassMetadata {

		private final AnnotationMetadata annotationMetadata;
		private final ClassMetadata classMetadata;

		MethodsMetadataWrapper(AnnotationMetadata annotationMetadata, ClassMetadata classMetadata) {
			this.annotationMetadata = annotationMetadata;
			this.classMetadata = classMetadata;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.type.MethodsMetadata#getMethods()
		 */
		@Override
		public Set<MethodMetadata> getMethods() {
			return annotationMetadata.getDeclaredMethods();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.type.MethodsMetadata#getMethods(java.lang.String)
		 */
		@Override
		public Set<MethodMetadata> getMethods(String name) {
			return annotationMetadata.getDeclaredMethods().stream().filter(it -> it.getMethodName().equals(name))
					.collect(Collectors.toSet());
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.ClassMetadata#getClassName()
		 */
		@Override
		public String getClassName() {
			return classMetadata.getClassName();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.ClassMetadata#isInterface()
		 */
		@Override
		public boolean isInterface() {
			return classMetadata.isInterface();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.ClassMetadata#isAnnotation()
		 */
		@Override
		public boolean isAnnotation() {
			return classMetadata.isAnnotation();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.ClassMetadata#isAbstract()
		 */
		@Override
		public boolean isAbstract() {
			return classMetadata.isAbstract();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.ClassMetadata#isConcrete()
		 */
		@Override
		public boolean isConcrete() {
			return classMetadata.isConcrete();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.ClassMetadata#isFinal()
		 */
		@Override
		public boolean isFinal() {
			return classMetadata.isFinal();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.ClassMetadata#isIndependent()
		 */
		@Override
		public boolean isIndependent() {
			return classMetadata.isIndependent();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.ClassMetadata#hasEnclosingClass()
		 */
		@Override
		public boolean hasEnclosingClass() {
			return classMetadata.hasEnclosingClass();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.ClassMetadata#getEnclosingClassName()
		 */
		@Override
		@Nullable
		public String getEnclosingClassName() {
			return classMetadata.getEnclosingClassName();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.ClassMetadata#hasSuperClass()
		 */
		@Override
		public boolean hasSuperClass() {
			return classMetadata.hasSuperClass();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.ClassMetadata#getSuperClassName()
		 */
		@Override
		@Nullable
		public String getSuperClassName() {
			return classMetadata.getSuperClassName();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.ClassMetadata#getInterfaceNames()
		 */
		@Override
		public String[] getInterfaceNames() {
			return classMetadata.getInterfaceNames();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.ClassMetadata#getMemberClassNames()
		 */
		@Override
		public String[] getMemberClassNames() {
			return classMetadata.getMemberClassNames();
		}
	}
}
