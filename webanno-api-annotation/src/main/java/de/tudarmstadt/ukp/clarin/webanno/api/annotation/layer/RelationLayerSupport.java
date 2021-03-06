/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer;

import static java.util.Arrays.asList;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.ArcAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.RelationRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.Renderer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

@Component
public class RelationLayerSupport
    implements LayerSupport<ArcAdapter>, InitializingBean
{
    private final FeatureSupportRegistry featureSupportRegistry;
    private final ApplicationEventPublisher eventPublisher;
    private final AnnotationSchemaService schemaService;

    private String layerSupportId;
    private List<LayerType> types;

    @Autowired
    public RelationLayerSupport(FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher, AnnotationSchemaService aSchemaService)
    {
        featureSupportRegistry = aFeatureSupportRegistry;
        eventPublisher = aEventPublisher;
        schemaService = aSchemaService;
    }

    @Override
    public String getId()
    {
        return layerSupportId;
    }

    @Override
    public void setBeanName(String aBeanName)
    {
        layerSupportId = aBeanName;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception
    {
        types = asList(new LayerType(WebAnnoConst.RELATION_TYPE, "Relation", layerSupportId));
    }
    
    @Override
    public List<LayerType> getSupportedLayerTypes()
    {
        return types;
    }
    
    @Override
    public boolean accepts(AnnotationLayer aLayer)
    {
        return WebAnnoConst.RELATION_TYPE.equals(aLayer.getType());
    }
    
    @Override
    public ArcAdapter createAdapter(AnnotationLayer aLayer)
    {
        ArcAdapter adapter = new ArcAdapter(featureSupportRegistry, eventPublisher, aLayer,
                aLayer.getId(), aLayer.getName(), WebAnnoConst.FEAT_REL_TARGET,
                WebAnnoConst.FEAT_REL_SOURCE,
                aLayer.getAttachFeature() == null ? null : aLayer.getAttachFeature().getName(),
                aLayer.getAttachType().getName(), schemaService.listAnnotationFeature(aLayer));

        adapter.setCrossMultipleSentence(aLayer.isCrossSentence());
        adapter.setAllowStacking(aLayer.isAllowStacking());

        return adapter;
    }
    
    @Override
    public void generateTypes(TypeSystemDescription aTsd, AnnotationLayer aLayer)
    {
        TypeDescription td = aTsd.addType(aLayer.getName(), "", CAS.TYPE_NAME_ANNOTATION);
        AnnotationLayer attachType = aLayer.getAttachType();

        td.addFeature(WebAnnoConst.FEAT_REL_TARGET, "", attachType.getName());
        td.addFeature(WebAnnoConst.FEAT_REL_SOURCE, "", attachType.getName());

        generateFeatures(aTsd, td, aLayer);
    }
    
    void generateFeatures(TypeSystemDescription aTSD, TypeDescription aTD,
            AnnotationLayer aLayer)
    {
        List<AnnotationFeature> features = schemaService.listAnnotationFeature(aLayer);
        for (AnnotationFeature feature : features) {
            FeatureSupport<?> fs = featureSupportRegistry.getFeatureSupport(feature);
            fs.generateFeature(aTSD, aTD, feature);
        }
    }
    
    @Override
    public Renderer getRenderer(AnnotationLayer aLayer)
    {
        return new RelationRenderer(createAdapter(aLayer), featureSupportRegistry);
    }
}
